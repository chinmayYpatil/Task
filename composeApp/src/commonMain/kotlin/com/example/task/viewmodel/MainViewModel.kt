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

    fun startSampleTaskFlow() {
        navigateTo(Screen.NoiseTest)
    }
}