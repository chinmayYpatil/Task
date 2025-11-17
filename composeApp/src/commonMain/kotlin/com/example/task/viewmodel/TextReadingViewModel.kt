package com.example.task.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.task.data.TextReadingTask
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
private const val TAG = "TextReadingApp_VM"

const val MIN_DURATION_SEC = 10
const val MAX_DURATION_SEC = 20

enum class RecordingState {
    IDLE,
    LOADING_TEXT,
    READY_TO_RECORD,
    RECORDING,
    REVIEW
}

data class CheckboxState(
    val noNoise: Boolean = false,
    val noMistakes: Boolean = false,
    val hindiCheck: Boolean = false // "Beech me koi galti nahi hai"
) {
    val allChecked: Boolean
        get() = noNoise && noMistakes && hindiCheck
}

data class TextReadingUiState(
    val passageText: String = "Loading passage...",
    val passageWords: List<String> = emptyList(),
    val lastSpokenWordIndex: Int = -1,
    val recordingState: RecordingState = RecordingState.LOADING_TEXT,
    val elapsedTime: Int = 0, // in seconds
    val lastRecordedDuration: Int? = null,
    val recordedAudioPath: String? = null,
    val errorMessage: String? = null,
    val checkboxState: CheckboxState = CheckboxState(),
    val isPlayingAudio: Boolean = false, // Playback status
    val playbackPositionMs: Int = 0, // Current playback position in ms
    val manualSeekPositionMs: Int = 0 // NEW: Position set by slider when not playing
) {
    val isSubmitEnabled: Boolean
        get() = recordingState == RecordingState.REVIEW && lastRecordedDuration != null && errorMessage == null && checkboxState.allChecked
}

class TextReadingViewModel(
    private val mainViewModel: MainViewModel,
    private val repository: TaskRepository = TaskRepository(),
    private val recorder: AudioRecorder = getAudioRecorder()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TextReadingUiState())
    val uiState: StateFlow<TextReadingUiState> = _uiState.asStateFlow()

    private var mockWordHighlightJob: Job? = null
    private var timerJob: Job? = null
    @OptIn(ExperimentalTime::class)
    private var recordingStartTime: Long = 0

    init {
        // Collect actual recording state from the recorder mock/impl
        viewModelScope.launch {
            recorder.isRecording.collectLatest { isRecording ->
                if (isRecording && _uiState.value.recordingState != RecordingState.RECORDING) {
                    // Start timer/highlighter when recorder reports start
                    startWordHighlightAndTimer()
                } else if (!isRecording && _uiState.value.recordingState == RecordingState.RECORDING) {
                    // Recorder stopped externally (e.g., auto-stop by platform), finalize manually
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

        // NEW: Collect playback position
        viewModelScope.launch {
            recorder.playbackPositionMs.collectLatest { position ->
                _uiState.update {
                    it.copy(
                        playbackPositionMs = position,
                        // If playback is not active and position is 0, update manual seek to 0
                        manualSeekPositionMs = if (!it.isPlayingAudio && position == 0) 0 else it.manualSeekPositionMs
                    )
                }
            }
        }

        fetchPassage()
    }

    private fun fetchPassage() {
        viewModelScope.launch {
            _uiState.update { it.copy(recordingState = RecordingState.LOADING_TEXT) }
            try {
                val text = repository.fetchTextPassage()
                // Keep words split as it's used to render the text word-by-word in the UI
                val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
                _uiState.update {
                    it.copy(
                        passageText = text,
                        passageWords = words,
                        lastSpokenWordIndex = -1, // Reset to initial state
                        recordingState = RecordingState.READY_TO_RECORD,
                        errorMessage = null,
                        lastRecordedDuration = null,
                        recordedAudioPath = null, // Reset path
                        checkboxState = CheckboxState(),
                        manualSeekPositionMs = 0 // Reset manual seek
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(passageText = "Error loading text: ${e.message}", recordingState = RecordingState.READY_TO_RECORD) }
            }
        }
    }

    /**
     * Toggles recording based on current state (single tap).
     */
    fun onMicClick() {
        if (_uiState.value.passageWords.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Cannot start recording: passage not loaded.") }
            return
        }

        when (_uiState.value.recordingState) {
            RecordingState.READY_TO_RECORD -> startRecording()
            RecordingState.RECORDING -> stopRecording()
            else -> {} // Ignore clicks in other states
        }
    }

    private fun startRecording() {
        logDebug(TAG, "Starting recording via AudioRecorder.")
        val path = recorder.startRecording()

        _uiState.update { it.copy(
            recordingState = RecordingState.RECORDING,
            elapsedTime = 0,
            errorMessage = null,
            lastSpokenWordIndex = -1, // Reset index
            recordedAudioPath = path, // Store path from mock
            manualSeekPositionMs = 0 // Reset manual seek
        ) }
    }

    @OptIn(ExperimentalTime::class)
    private fun startWordHighlightAndTimer() {
        logDebug(TAG, "Starting recording timer.")
        recordingStartTime = Clock.System.now().toEpochMilliseconds()

        // 1. Timer Job (updates elapsed time and handles auto-stop)
        // This is the only job running now.
        mockWordHighlightJob?.cancel()
        mockWordHighlightJob = viewModelScope.launch {
            while (true) {
                delay(50L) // Update frequently for smooth timer
                val currentMs = Clock.System.now().toEpochMilliseconds()
                val newElapsedTime = ((currentMs - recordingStartTime) / 1000).toInt()

                // 1. Update elapsed time for UI
                _uiState.update { it.copy(elapsedTime = newElapsedTime) }

                // 2. Auto-stop if exceeding max duration (20 seconds)
                if (newElapsedTime >= MAX_DURATION_SEC) {
                    logDebug(TAG, "Auto-stopping due to max duration.")
                    // Stop the actual recorder, which will trigger stopRecordingInternal
                    recorder.stopRecording()
                    break
                }
            }
        }
    }

    private fun stopRecording() {
        val finalDuration = recorder.stopRecording() // Call actual stop
        stopRecordingInternal(isManualStop = true, duration = finalDuration)
    }

    private fun stopRecordingInternal(isManualStop: Boolean, duration: Int? = null) {
        if (_uiState.value.recordingState != RecordingState.RECORDING) return

        mockWordHighlightJob?.cancel() // Stop timer job

        // Get duration from actual recorder call or use existing elapsedTime if unexpected stop
        val finalDuration = duration ?: _uiState.value.elapsedTime

        logDebug(TAG, "Recording stopped. Final Duration: $finalDuration seconds. Manual: $isManualStop")

        val errorMessage = when {
            finalDuration < MIN_DURATION_SEC -> "Recording too short (min ${MIN_DURATION_SEC}s)."
            finalDuration > MAX_DURATION_SEC -> "Recording too long (max ${MAX_DURATION_SEC}s)."
            else -> null
        }

        _uiState.update {
            it.copy(
                recordingState = RecordingState.REVIEW,
                lastRecordedDuration = if (errorMessage == null) finalDuration else null,
                errorMessage = errorMessage,
                elapsedTime = finalDuration,
                lastSpokenWordIndex = -1, // Reset the index
                manualSeekPositionMs = 0 // Reset manual seek
            )
        }
    }

    fun onRecordAgainClick() {
        recorder.stopPlayback() // Ensure playback is stopped before restarting task
        _uiState.update {
            it.copy(
                recordingState = RecordingState.READY_TO_RECORD,
                lastRecordedDuration = null,
                errorMessage = null,
                elapsedTime = 0,
                checkboxState = CheckboxState(),
                lastSpokenWordIndex = -1, // Reset the index
                manualSeekPositionMs = 0 // Reset manual seek
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

    @OptIn(ExperimentalTime::class)
    fun onSubmitClick() {
        val state = _uiState.value
        if (!state.isSubmitEnabled || state.lastRecordedDuration == null) return

        viewModelScope.launch {
            val task = TextReadingTask(
                text = state.passageText,
                audioPath = state.recordedAudioPath ?: "unknown_path",
                durationSec = state.lastRecordedDuration,
                timestamp = Clock.System.now()
            )
            repository.saveTextReadingTask(task)
            mainViewModel.navigateTo(Screen.TaskSelection) // Navigate back to Task Selection
        }
    }

    /**
     * Toggles playback. If playing, stops. If stopped, plays from the manual seek position.
     */
    fun onPlayClick() {
        val state = _uiState.value
        val path = state.recordedAudioPath
        if (path != null) {
            if (state.isPlayingAudio) {
                recorder.stopPlayback()
            } else {
                // If not playing, start playing from the manual seek position
                recorder.playRecording(path, state.manualSeekPositionMs)
            }
        } else {
            logDebug(TAG, "Cannot play: No recorded audio path found.")
        }
    }

    /**
     * Calculates the target time in milliseconds and either seeks the active player
     * or stores the position for next playback.
     * @param progressFraction A float between 0.0f and 1.0f.
     */
    fun onSeek(progressFraction: Float) {
        val state = _uiState.value
        val durationSec = state.lastRecordedDuration ?: 0
        val totalDurationMs = durationSec * 1000
        if (totalDurationMs > 0) {
            val targetMs = (progressFraction * totalDurationMs).toInt()

            if (state.isPlayingAudio) {
                // If playing, seek immediately
                recorder.seekTo(targetMs)
            } else {
                // If not playing, store the position for the next playback
                _uiState.update { it.copy(manualSeekPositionMs = targetMs) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // This is the crucial cleanup step that should now be reliably called
        // due to the fix in MainViewModel.popBack()
        recorder.stopRecording()
        recorder.stopPlayback()
    }
}