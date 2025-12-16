package com.example.task.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.task.data.AppTask
import com.example.task.data.ImageDescriptionTask
import com.example.task.data.PhotoCaptureTask
import com.example.task.data.TextReadingTask
import com.example.task.platform.AudioRecorder
import com.example.task.platform.getAudioRecorder
import com.example.task.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

data class HistoryUiState(
    val tasks: List<AppTask> = emptyList(),
    val totalTasks: Int = 0,
    val totalDurationFormatted: String = "0m 0s",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // NEW: Detail View State
    val selectedTask: AppTask? = null,
    // NEW: Playback State (from AudioRecorder flow)
    val isPlayingAudio: Boolean = false,
    val playbackPositionMs: Int = 0, // Current playback position in ms
    val manualSeekPositionMs: Int = 0 // Position set by slider when not playing
)

@OptIn(ExperimentalTime::class)
class TaskHistoryViewModel(
    private val repository: TaskRepository = TaskRepository(),
    private val recorder: AudioRecorder = getAudioRecorder()
) : ViewModel() {

    // FIX: Explicitly specify the generic type for MutableStateFlow
    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadTasks()

        // Collect playback state from the recorder
        viewModelScope.launch {
            recorder.isPlaying.collectLatest { isPlaying ->
                // FIX: Use named arguments in copy when necessary for clarity, though it should work without.
                _uiState.update { it.copy(isPlayingAudio = isPlaying) }
                // If playback stops, reset manual seek position if it reached the end (position 0)
                if (!isPlaying && _uiState.value.playbackPositionMs == 0) {
                    _uiState.update { it.copy(manualSeekPositionMs = 0) }
                }
            }
        }

        // Collect playback position
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
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            // Ensure no playback is active before reloading data
            recorder.stopPlayback()
            try {
                // NEW: Force the repository to reload its in-memory list from disk on every loadTasks call
                repository.reloadCompletedTasks()

                // Fetch and sort by newest first - FIX: Use timestampMs
                val tasks = repository.fetchCompletedTasks().sortedByDescending { it.timestampMs }

                // Calculate total duration (only from tasks with valid duration)
                val totalDurationSeconds = tasks.sumOf { it.durationSec ?: 0 }

                // Format duration
                val totalDuration = totalDurationSeconds.seconds
                val minutes = totalDuration.inWholeMinutes
                val seconds = totalDuration.inWholeSeconds % 60
                val formattedDuration = "${minutes}m ${seconds}s"

                _uiState.update {
                    it.copy(
                        tasks = tasks,
                        totalTasks = tasks.size,
                        totalDurationFormatted = formattedDuration,
                        isLoading = false,
                        selectedTask = null, // Deselect task on refresh
                        manualSeekPositionMs = 0
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load history: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Sets or toggles the currently selected task for detail viewing/playback.
     */
    fun selectTask(task: AppTask) {
        // Stop playback if switching tasks
        recorder.stopPlayback()

        _uiState.update { currentState ->
            if (currentState.selectedTask?.taskId == task.taskId) {
                // Collapse if already selected
                currentState.copy(selectedTask = null, manualSeekPositionMs = 0)
            } else {
                // Expand and show details
                currentState.copy(selectedTask = task, manualSeekPositionMs = 0)
            }
        }
    }

    /**
     * Toggles playback for the currently selected task's audio.
     */
    fun onPlayClick() {
        val state = _uiState.value
        val task = state.selectedTask
        // Get audio path from the selected task based on its type
        val path = when (task) {
            is TextReadingTask -> task.audioPath
            is ImageDescriptionTask -> task.audioPath
            is PhotoCaptureTask -> task.audioPath
            else -> null
        }

        // Ensure path and duration are valid before attempting playback
        if (path != null && task?.durationSec != null && task.durationSec!! > 0) {
            if (state.isPlayingAudio) {
                recorder.stopPlayback()
            } else {
                // Start playing from the manual seek position
                recorder.playRecording(path, state.manualSeekPositionMs)
            }
        } else {
            println("Cannot play: No valid recorded audio path found for selected task.")
        }
    }

    /**
     * Calculates the target time in milliseconds and either seeks the active player
     * or stores the position for next playback.
     * @param progressFraction A float between 0.0f and 1.0f.
     */
    fun onSeek(progressFraction: Float) {
        val state = _uiState.value
        val durationSec = state.selectedTask?.durationSec ?: 0
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

    /**
     * Public method to explicitly stop audio playback, used when navigating away from the screen.
     */
    fun stopPlayback() {
        recorder.stopPlayback()
    }

    override fun onCleared() {
        super.onCleared()
        recorder.stopPlayback()
    }
}