package com.example.task.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.task.data.AppTask
import com.example.task.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

data class HistoryUiState(
    val tasks: List<AppTask> = emptyList(),
    val totalTasks: Int = 0,
    val totalDurationFormatted: String = "0m 0s",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class TaskHistoryViewModel(
    private val repository: TaskRepository = TaskRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    @OptIn(ExperimentalTime::class)
    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Fetch and sort by newest first
                val tasks = repository.fetchCompletedTasks().sortedByDescending { it.timestamp }

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
                        isLoading = false
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
}