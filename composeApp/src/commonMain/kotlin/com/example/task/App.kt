package com.example.task

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons // <-- ADD THIS IMPORT
import androidx.compose.material.icons.automirrored.filled.ArrowBack // <-- ADD THIS IMPORT
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.task.navigation.Screen
import com.example.task.screen.NoiseTestScreen
import com.example.task.screen.TaskSelectionScreen
import com.example.task.screen.rememberTaskSelectionViewModel
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    title: String,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (canNavigateBack) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            content()
        }
    }
}


@Composable
fun TaskApp() {
    MaterialTheme {
        val mainViewModel = rememberMainViewModel()
        val uiState by mainViewModel.uiState.collectAsState()

        val currentScreen = uiState.currentScreen
        val title = when (currentScreen) {
            Screen.Start -> ""
            Screen.NoiseTest -> "Noise Test"
            Screen.TaskSelection -> "Select Task"
            else -> "App" // Placeholder for other screens
        }

        // Define back action logic
        val onBack: () -> Unit = {
            // REQUIRED: If popBack returns false (i.e., we are on the root screen - Start),
            // the Android/iOS system should handle closing the app.
            mainViewModel.popBack()
        }
        val canNavigateBack = currentScreen != Screen.Start

        // Global Navigation Host (MainView)
        AppScreen(
            title = title,
            canNavigateBack = canNavigateBack,
            onNavigateBack = onBack
        ) {
            when (currentScreen) {
                Screen.Start -> {
                    val startViewModel = rememberStartViewModel(mainViewModel)
                    StartScreen(viewModel = startViewModel)
                }
                Screen.NoiseTest -> NoiseTestScreen(mainViewModel = mainViewModel)
                Screen.TaskSelection -> TaskSelectionScreen(mainViewModel = mainViewModel)
                else -> {}
            }
        }
    }
}


@Preview
@Composable
fun TaskAppAndroidPreview() {
    TaskApp()
}