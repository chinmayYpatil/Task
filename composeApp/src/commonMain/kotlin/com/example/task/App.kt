package com.example.task

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialTheme
import com.example.task.navigation.Screen
import com.example.task.viewmodel.MainViewModel
import com.example.task.viewmodel.StartViewModel
import com.example.task.screen.StartScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

// Helper function to get the MainViewModel.
@Composable
fun rememberMainViewModel(): MainViewModel {
    return viewModel()
}

// Factory to create the StartViewModel, injecting the MainViewModel dependency.
@Composable
fun rememberStartViewModel(mainViewModel: MainViewModel): StartViewModel {
    return remember { StartViewModel(mainViewModel) }
}


@Composable
fun TaskApp() {
    MaterialTheme {
        val mainViewModel = rememberMainViewModel()
        val uiState by mainViewModel.uiState.collectAsState()

        // Global Navigation Host (MainView)
        when (uiState.currentScreen) {
            Screen.Start -> {
                val startViewModel = rememberStartViewModel(mainViewModel)
                StartScreen(viewModel = startViewModel)
            }

            else -> {}
        }
    }
}


@Preview
@Composable
fun TaskAppAndroidPreview() {
    TaskApp()
}