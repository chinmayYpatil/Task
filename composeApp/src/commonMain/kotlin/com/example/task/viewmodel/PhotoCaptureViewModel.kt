package com.example.task.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.task.data.PhotoCaptureTask
import com.example.task.navigation.Screen
import com.example.task.platform.AudioRecorder
import com.example.task.platform.CameraLauncher
import com.example.task.platform.getCameraLauncher
import com.example.task.platform.getAudioRecorder
import com.example.task.repository.TaskRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Duration.Companion.seconds

// Mock logger
private fun logDebug(tag: String, message: String) {
    println("DEBUG | $tag: $message")
}
private const val TAG = "PhotoCaptureApp_VM" // Log Tag

// Re-using duration constraints from other task ViewModels
const val PHOTO_MIN_DURATION_SEC = 10
const val PHOTO_MAX_DURATION_SEC = 20

enum class PhotoCaptureState {
    PERMISSION_CHECK, // Waiting for camera permission check/request
    READY_TO_CAPTURE, // Permission granted, waiting for photo capture
    PHOTO_PREVIEW,    // Photo captured, waiting for description/audio
    RECORDING,        // Recording audio description
    REVIEW            // Ready for submission
}

data class PhotoCaptureCheckboxState(
    val noNoise: Boolean = false,
    val noMistakes: Boolean = false,
    val hindiCheck: Boolean = false
) {
    val allChecked: Boolean
        get() = noNoise && noMistakes && hindiCheck
}

data class PhotoCaptureUiState(
    val state: PhotoCaptureState = PhotoCaptureState.PERMISSION_CHECK,
    val capturedImagePath: String? = null,
    val textDescription: String = "",
    val recordedAudioPath: String? = null,
    val lastRecordedDuration: Int? = null,
    val elapsedTime: Int = 0, // in seconds (for audio)
    val errorMessage: String? = null,
    val checkboxState: PhotoCaptureCheckboxState = PhotoCaptureCheckboxState(),
    val isPlayingAudio: Boolean = false,
    val playbackPositionMs: Int = 0,
    val manualSeekPositionMs: Int = 0
) {
    val isSubmitEnabled: Boolean
        get() = state == PhotoCaptureState.REVIEW &&
                capturedImagePath != null &&
                errorMessage == null &&
                checkboxState.allChecked
}

class PhotoCaptureViewModel(
    private val mainViewModel: MainViewModel,
    private val repository: TaskRepository = TaskRepository(),
    private val recorder: AudioRecorder = getAudioRecorder(),
    private val cameraLauncher: CameraLauncher = getCameraLauncher() // <-- NEW INJECTION
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoCaptureUiState())
    val uiState: StateFlow<PhotoCaptureUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    @OptIn(ExperimentalTime::class)
    private var recordingStartTime: Long = 0

    init {
        // Mock permission check, assumes success for KMP UI demonstration
        viewModelScope.launch {
            delay(200L)
            _uiState.update { it.copy(state = PhotoCaptureState.READY_TO_CAPTURE) }
        }

        // NEW: Collect results from the Camera Launcher (receives actual file path)
        viewModelScope.launch {
            cameraLauncher.capturedImagePath.collectLatest { path ->
                logDebug(TAG, "Camera Result Received. Path: $path")
                _uiState.update {
                    it.copy(
                        state = PhotoCaptureState.PHOTO_PREVIEW,
                        capturedImagePath = path,
                        errorMessage = null // Clear any prior error
                    )
                }
            }
        }

        // Collect audio recorder states (similar to other ViewModels)
        viewModelScope.launch {
            recorder.isRecording.collectLatest { isRecording ->
                if (isRecording && _uiState.value.state != PhotoCaptureState.RECORDING) {
                    startTimer()
                } else if (!isRecording && _uiState.value.state == PhotoCaptureState.RECORDING) {
                    stopRecordingInternal(isManualStop = false, duration = null)
                }
            }
        }

        viewModelScope.launch {
            recorder.isPlaying.collectLatest { isPlaying ->
                _uiState.update { it.copy(isPlayingAudio = isPlaying) }
            }
        }

        viewModelScope.launch {
            recorder.playbackPositionMs.collectLatest { position ->
                _uiState.update {
                    it.copy(
                        playbackPositionMs = position,
                        manualSeekPositionMs = if (!it.isPlayingAudio && position == 0) 0 else it.manualSeekPositionMs
                    )
                }
            }
        }
    }

    // --- UI Actions ---

    // UPDATED: Now calls the injected launcher
    fun onCaptureImageClick() {
        if (_uiState.value.state != PhotoCaptureState.READY_TO_CAPTURE) return
        logDebug(TAG, "Attempting to launch native camera.")

        // This triggers the platform-specific camera intent/launcher
        cameraLauncher.launch()

        // UI update will happen when the result flows back via cameraLauncher.capturedImagePath
    }

    fun onTextDescriptionChange(newText: String) {
        _uiState.update { it.copy(textDescription = newText) }
    }

    // ... rest of the ViewModel logic remains the same (audio, seek, submit) ...

    fun onMicClick() {
        when (_uiState.value.state) {
            PhotoCaptureState.PHOTO_PREVIEW -> startRecording()
            PhotoCaptureState.RECORDING -> stopRecording()
            else -> {}
        }
    }

    fun onRecordAgainClick() {
        recorder.stopPlayback()
        // Reset only audio/description fields, keep the captured image
        _uiState.update {
            it.copy(
                state = PhotoCaptureState.PHOTO_PREVIEW,
                recordedAudioPath = null,
                lastRecordedDuration = null,
                elapsedTime = 0,
                errorMessage = null,
                checkboxState = PhotoCaptureCheckboxState(
                    noNoise = it.checkboxState.noNoise, // Keep previous state for mandatory checks if possible
                    noMistakes = it.checkboxState.noMistakes,
                    hindiCheck = it.checkboxState.hindiCheck
                ),
                manualSeekPositionMs = 0
            )
        }
    }

    fun onRetakePhotoClick() {
        recorder.stopPlayback()
        _uiState.update {
            it.copy(
                state = PhotoCaptureState.READY_TO_CAPTURE,
                capturedImagePath = null,
                textDescription = "",
                recordedAudioPath = null,
                lastRecordedDuration = null,
                elapsedTime = 0,
                errorMessage = null,
                checkboxState = PhotoCaptureCheckboxState(),
                manualSeekPositionMs = 0
            )
        }
    }

    // --- Audio Logic ---

    private fun startRecording() {
        logDebug(TAG, "Starting audio recording.")
        val path = recorder.startRecording()

        _uiState.update { it.copy(
            state = PhotoCaptureState.RECORDING,
            elapsedTime = 0,
            errorMessage = null,
            recordedAudioPath = path,
            manualSeekPositionMs = 0
        ) }
    }

    @OptIn(ExperimentalTime::class)
    private fun startTimer() {
        logDebug(TAG, "Starting recording timer.")
        recordingStartTime = Clock.System.now().toEpochMilliseconds()

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(50L)
                val currentMs = Clock.System.now().toEpochMilliseconds()
                val newElapsedTime = ((currentMs - recordingStartTime) / 1000).toInt()

                _uiState.update { it.copy(elapsedTime = newElapsedTime) }

                if (newElapsedTime >= PHOTO_MAX_DURATION_SEC) {
                    logDebug(TAG, "Auto-stopping due to max duration.")
                    recorder.stopRecording()
                    break
                }
            }
        }
    }

    private fun stopRecording() {
        val finalDuration = recorder.stopRecording()
        stopRecordingInternal(isManualStop = true, duration = finalDuration)
    }

    private fun stopRecordingInternal(isManualStop: Boolean, duration: Int? = null) {
        if (_uiState.value.state != PhotoCaptureState.RECORDING) return

        timerJob?.cancel()

        val finalDuration = duration ?: _uiState.value.elapsedTime

        logDebug(TAG, "Recording stopped. Final Duration: $finalDuration seconds. Manual: $isManualStop")

        val errorMessage = when {
            finalDuration > 0 && finalDuration < PHOTO_MIN_DURATION_SEC -> "Recording too short (min ${PHOTO_MIN_DURATION_SEC}s)."
            finalDuration > PHOTO_MAX_DURATION_SEC -> "Recording too long (max ${PHOTO_MAX_DURATION_SEC}s)."
            else -> null
        }

        // Transition to REVIEW state, handling the audio validation
        val newState = if (errorMessage == null) PhotoCaptureState.REVIEW else PhotoCaptureState.PHOTO_PREVIEW

        _uiState.update {
            it.copy(
                state = newState,
                lastRecordedDuration = if (errorMessage == null) finalDuration else null,
                errorMessage = errorMessage,
                elapsedTime = finalDuration,
                manualSeekPositionMs = 0
            )
        }
    }

    fun onPlayClick() {
        val state = _uiState.value
        val path = state.recordedAudioPath
        if (path != null) {
            if (state.isPlayingAudio) {
                recorder.stopPlayback()
            } else {
                recorder.playRecording(path, state.manualSeekPositionMs)
            }
        } else {
            logDebug(TAG, "Cannot play: No recorded audio path found.")
        }
    }

    fun onSeek(progressFraction: Float) {
        val state = _uiState.value
        val durationSec = state.lastRecordedDuration ?: 0
        val totalDurationMs = durationSec * 1000
        if (totalDurationMs > 0) {
            val targetMs = (progressFraction * totalDurationMs).toInt()

            if (state.isPlayingAudio) {
                recorder.seekTo(targetMs)
            } else {
                _uiState.update { it.copy(manualSeekPositionMs = targetMs) }
            }
        }
    }

    // --- Submission ---

    @OptIn(ExperimentalTime::class)
    fun onSubmitClick() {
        val state = _uiState.value
        if (!state.isSubmitEnabled || state.capturedImagePath == null) return

        val hasAudio = (state.lastRecordedDuration ?: 0) > 0
        val hasText = state.textDescription.isNotBlank()

        if (!hasAudio && !hasText) {
            _uiState.update { it.copy(errorMessage = "Please provide a description either via text or audio.") }
            return
        }

        viewModelScope.launch {
            val task = PhotoCaptureTask(
                imagePath = state.capturedImagePath,
                audioPath = state.recordedAudioPath.takeIf { hasAudio },
                durationSec = state.lastRecordedDuration.takeIf { hasAudio },
                textDescription = state.textDescription.takeIf { hasText },
                timestampMs = Clock.System.now().toEpochMilliseconds()
            )
            repository.savePhotoCaptureTask(task)
            mainViewModel.navigateTo(Screen.TaskSelection)
        }
    }

    fun onCheckboxToggled(index: Int, isChecked: Boolean) {
        _uiState.update {
            val newState = when (index) {
                0 -> it.checkboxState.copy(noNoise = isChecked)
                1 -> it.checkboxState.copy(noMistakes = isChecked)
                2 -> it.checkboxState.copy(hindiCheck = isChecked)
                else -> it.checkboxState
            }
            it.copy(checkboxState = newState)
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorder.stopRecording()
        recorder.stopPlayback()
    }
}