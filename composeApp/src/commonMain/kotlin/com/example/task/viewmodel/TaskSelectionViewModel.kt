package com.example.task.viewmodel

import androidx.lifecycle.ViewModel
import com.example.task.navigation.Screen

// No complex state needed for this screen, mostly navigation.
class TaskSelectionViewModel(
    private val mainViewModel: MainViewModel
) : ViewModel() {

    fun onTaskSelected(screen: Screen) {
        mainViewModel.navigateTo(screen)
    }

    fun onBackClick() {
        mainViewModel.navigateTo(Screen.Start)
    }
}