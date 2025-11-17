package com.example.task.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.task.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Data class to hold the overall UI state (Navigation)
data class MainUiState(
    val currentScreen: Screen = Screen.Start
)

// Main Navigation ViewModel
class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun navigateTo(screen: Screen) {
        viewModelScope.launch {
            _uiState.update { it.copy(currentScreen = screen) }
        }
    }

    /**
     * Simulates popping the back stack.
     * Only supports navigation back to a fixed set of screens for this prototype.
     */
    fun popBack(): Boolean {
        return when (_uiState.value.currentScreen) {
            Screen.NoiseTest -> {
                navigateTo(Screen.Start)
                true
            }
            Screen.TaskSelection -> {
                navigateTo(Screen.NoiseTest)
                true
            }
            Screen.TextReading -> {
                navigateTo(Screen.TaskSelection)
                true
            }
            // NEW: Add navigation back from ImageDescription to TaskSelection
            Screen.ImageDescription -> {
                navigateTo(Screen.TaskSelection)
                true
            }
            // For the Start screen, this should be handled by the system (close app/go home)
            Screen.Start -> false
            else -> false
        }
    }

    fun startSampleTaskFlow() {
        navigateTo(Screen.NoiseTest)
    }
}