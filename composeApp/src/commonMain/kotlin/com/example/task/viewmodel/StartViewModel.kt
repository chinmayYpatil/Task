package com.example.task.viewmodel

import androidx.lifecycle.ViewModel
import com.example.task.navigation.Screen

// UI State for Start Screen
data class StartUiState(
    val isButtonEnabled: Boolean = true
)

// ViewModel
class StartViewModel(
    private val mainViewModel: MainViewModel
) : ViewModel() {

    val uiState = StartUiState()

    fun onStartTaskClick() {
        mainViewModel.startSampleTaskFlow()
    }
}