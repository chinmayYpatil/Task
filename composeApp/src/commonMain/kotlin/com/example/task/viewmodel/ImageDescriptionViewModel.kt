package com.example.task.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.task.data.ImageDescriptionTask
import com.example.task.navigation.Screen
import com.example.task.platform.AudioRecorder
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
private const val TAG = "ImageDescApp_VM" // Log Tag

const val IMAGE_MIN_DURATION_SEC = 10
const val IMAGE_MAX_DURATION_SEC = 20

enum class ImageRecordingState {
    IDLE,
    LOADING_TASK,
    READY_TO_RECORD,
    RECORDING,
    REVIEW
}

data class ImageDescriptionCheckboxState(
    val noNoise: Boolean = false,
    val noMistakes: Boolean = false,
    val hindiCheck: Boolean = false // "Beech me koi galti nahi hai"
) {
    val allChecked: Boolean
        get() = noNoise && noMistakes && hindiCheck
}

data class ImageDescriptionUiState(
    val instruction: String = "Loading task...",
    val imageUrl: String? = null,
    val recordingState: ImageRecordingState = ImageRecordingState.LOADING_TASK,
    val elapsedTime: Int = 0, // in seconds
    val lastRecordedDuration: Int? = null,
    val recordedAudioPath: String? = null,
    val errorMessage: String? = null,
    val checkboxState: ImageDescriptionCheckboxState = ImageDescriptionCheckboxState(),
    val isPlayingAudio: Boolean = false, // Playback status
    val playbackPositionMs: Int = 0, // Current playback position in ms
    val manualSeekPositionMs: Int = 0
) {
    val isSubmitEnabled: Boolean
        get() = recordingState == ImageRecordingState.REVIEW && lastRecordedDuration != null && errorMessage == null && checkboxState.allChecked
}

class ImageDescriptionViewModel(
    private val mainViewModel: MainViewModel,
    private val repository: TaskRepository = TaskRepository(),
    private val recorder: AudioRecorder = getAudioRecorder()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageDescriptionUiState())
    val uiState: StateFlow<ImageDescriptionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    @OptIn(ExperimentalTime::class)
    private var recordingStartTime: Long = 0

    init {
        // Collect actual recording state from the recorder mock/impl
        viewModelScope.launch {
            recorder.isRecording.collectLatest { isRecording ->
                if (isRecording && _uiState.value.recordingState != ImageRecordingState.RECORDING) {
                    startTimer()
                } else if (!isRecording && _uiState.value.recordingState == ImageRecordingState.RECORDING) {
                    stopRecordingInternal(isManualStop = false, duration = null)
                }
            }
        }

        // Collect playback state from the recorder
        viewModelScope.launch {
            recorder.isPlaying.collectLatest { isPlaying ->
                _uiState.update { it.copy(isPlayingAudio = isPlaying) }
            }
        }

        // Collect playback position
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

        // Removed call to fetchImageTaskData() - replaced by external call to startNewTask()
    }

    // Public function to explicitly reset and start a new Image Description task by fetching the image and instruction
    fun startNewTask() {
        recorder.stopPlayback() // Ensure playback is stopped when entering a new task
        viewModelScope.launch {
            // Reset state
            _uiState.update {
                it.copy(
                    instruction = "Loading task...",
                    imageUrl = null,
                    recordingState = ImageRecordingState.LOADING_TASK,
                    elapsedTime = 0,
                    lastRecordedDuration = null,
                    recordedAudioPath = null,
                    errorMessage = null,
                    checkboxState = ImageDescriptionCheckboxState(),
                    manualSeekPositionMs = 0
                )
            }
            logDebug(TAG, "Starting image task data fetch...")
            try {
                val (instruction, imageUrl) = repository.fetchImageDescriptionTaskData()

                val success = imageUrl != null && imageUrl.isNotEmpty()

                logDebug(TAG, "Image Task fetch complete. Success: $success. URL: $imageUrl")

                _uiState.update {
                    it.copy(
                        instruction = instruction,
                        imageUrl = imageUrl,
                        recordingState = if (success) ImageRecordingState.READY_TO_RECORD else ImageRecordingState.IDLE,
                        errorMessage = if (!success) "Error loading image task data." else null,
                        // rest of state already reset above
                    )
                }
            } catch (e: Exception) {
                val errorMsg = "Fatal Error fetching image task: ${e.message}"
                logDebug(TAG, errorMsg)
                _uiState.update { it.copy(instruction = errorMsg, recordingState = ImageRecordingState.IDLE, errorMessage = errorMsg) }
            }
        }
    }

    fun onMicClick() {
        if (_uiState.value.imageUrl == null) {
            _uiState.update { it.copy(errorMessage = "Cannot start recording: image task not loaded.") }
            return
        }

        when (_uiState.value.recordingState) {
            ImageRecordingState.READY_TO_RECORD -> startRecording()
            ImageRecordingState.RECORDING -> stopRecording()
            else -> {}
        }
    }

    private fun startRecording() {
        logDebug(TAG, "Starting recording via AudioRecorder.")
        val path = recorder.startRecording()

        _uiState.update { it.copy(
            recordingState = ImageRecordingState.RECORDING,
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

                if (newElapsedTime >= IMAGE_MAX_DURATION_SEC) {
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
        if (_uiState.value.recordingState != ImageRecordingState.RECORDING) return

        timerJob?.cancel()

        val finalDuration = duration ?: _uiState.value.elapsedTime

        logDebug(TAG, "Recording stopped. Final Duration: $finalDuration seconds. Manual: $isManualStop")

        val errorMessage = when {
            finalDuration < IMAGE_MIN_DURATION_SEC -> "Recording too short (min ${IMAGE_MIN_DURATION_SEC}s)."
            finalDuration > IMAGE_MAX_DURATION_SEC -> "Recording too long (max ${IMAGE_MAX_DURATION_SEC}s)."
            else -> null
        }

        _uiState.update {
            it.copy(
                recordingState = ImageRecordingState.REVIEW,
                lastRecordedDuration = if (errorMessage == null) finalDuration else null,
                errorMessage = errorMessage,
                elapsedTime = finalDuration,
                manualSeekPositionMs = 0
            )
        }
    }

    fun onRecordAgainClick() {
        recorder.stopPlayback()
        _uiState.update {
            it.copy(
                recordingState = ImageRecordingState.READY_TO_RECORD,
                lastRecordedDuration = null,
                errorMessage = null,
                elapsedTime = 0,
                checkboxState = ImageDescriptionCheckboxState(),
                manualSeekPositionMs = 0
            )
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

    /**
     * Public method to explicitly stop audio playback, used when navigating away from the screen.
     */
    fun stopPlayback() {
        recorder.stopPlayback()
    }

    @OptIn(ExperimentalTime::class)
    fun onSubmitClick() {
        val state = _uiState.value
        if (!state.isSubmitEnabled || state.lastRecordedDuration == null || state.imageUrl == null) return

        viewModelScope.launch {
            // FIX: Stop playback before navigation
            recorder.stopPlayback()

            val task = ImageDescriptionTask(
                imageUrl = state.imageUrl,
                audioPath = state.recordedAudioPath ?: "unknown_path",
                durationSec = state.lastRecordedDuration,
                timestampMs = Clock.System.now().toEpochMilliseconds()
            )
            repository.saveImageDescriptionTask(task)
            mainViewModel.navigateTo(Screen.TaskSelection)
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

    override fun onCleared() {
        super.onCleared()
        recorder.stopRecording()
        recorder.stopPlayback()
    }
}