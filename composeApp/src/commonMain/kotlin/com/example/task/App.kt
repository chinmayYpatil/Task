package com.example.task

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
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
import com.example.task.screen.TextReadingScreen
import com.example.task.screen.ImageDescriptionScreen
import com.example.task.screen.PhotoCaptureScreen
import com.example.task.screen.rememberTaskSelectionViewModel
import com.example.task.viewmodel.MainViewModel
import com.example.task.viewmodel.StartViewModel
import com.example.task.screen.StartScreen
import com.example.task.viewmodel.PhotoCaptureViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

// NEW IMPORTS for bottom navigation
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import com.example.task.screen.HistoryScreen

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

// NEW: Define the type alias for the PhotoCaptureViewModel factory
typealias PhotoCaptureViewModelFactory = @Composable (MainViewModel) -> PhotoCaptureViewModel

@Composable
fun rememberPhotoCaptureViewModel(mainViewModel: MainViewModel): PhotoCaptureViewModel {
    // This is the default common constructor fallback
    return remember { PhotoCaptureViewModel(mainViewModel = mainViewModel) }
}

@Composable
fun rememberPhotoCaptureViewModel(mainViewModel: MainViewModel, factory: PhotoCaptureViewModelFactory?): PhotoCaptureViewModel {
    // Use the provided factory if available (for platform injection), otherwise use the default common constructor
    return factory?.invoke(mainViewModel) ?: rememberPhotoCaptureViewModel(mainViewModel)
}


// REMOVED: AppScreen composable is removed, its functionality is merged into TaskApp.


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskApp(
    photoCaptureViewModelFactory: PhotoCaptureViewModelFactory? = null // <-- NEW PARAMETER
) {
    MaterialTheme {
        val mainViewModel = rememberMainViewModel()
        val uiState by mainViewModel.uiState.collectAsState()

        val currentScreen = uiState.currentScreen
        val title = when (currentScreen) {
            Screen.Start -> "Sample Task App"
            Screen.NoiseTest -> "Noise Test"
            Screen.TaskSelection -> "Select Task"
            Screen.TextReading -> "Text Reading Task"
            Screen.ImageDescription -> "Image Description Task"
            Screen.PhotoCapture -> "Photo Capture Task"
            Screen.History -> "Task History" // NEW Title
            else -> "App"
        }

        // --- Back Navigation Logic ---
        val onBack: () -> Unit = {
            mainViewModel.popBack()
        }
        // Only show back button if not on a root screen (Start or History)
        val canNavigateBack = currentScreen != Screen.Start && currentScreen != Screen.History

        // --- Bottom Navigation Logic ---
        val navItems = listOf(Screen.Start, Screen.History)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        if (canNavigateBack) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    navItems.forEach { screen ->
                        // Highlight logic: only Start and History are selectable from the bar
                        val isSelected = currentScreen == screen ||
                                (screen == Screen.Start && currentScreen != Screen.History && currentScreen != Screen.Start)

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        Screen.Start -> Icons.Filled.Home
                                        Screen.History -> Icons.Filled.History
                                        else -> Icons.Filled.Home
                                    },
                                    contentDescription = when (screen) {
                                        Screen.Start -> "Home"
                                        Screen.History -> "History"
                                        else -> ""
                                    }
                                )
                            },
                            label = {
                                Text(
                                    when (screen) {
                                        Screen.Start -> "Home"
                                        Screen.History -> "History"
                                        else -> ""
                                    }
                                )
                            },
                            selected = currentScreen == screen, // Highlight only if exactly on the root screen
                            onClick = {
                                mainViewModel.navigateTo(screen)
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                // Global Navigation Host
                when (currentScreen) {
                    Screen.Start -> {
                        val startViewModel = rememberStartViewModel(mainViewModel)
                        StartScreen(viewModel = startViewModel)
                    }
                    Screen.NoiseTest -> NoiseTestScreen(mainViewModel = mainViewModel)
                    Screen.TaskSelection -> TaskSelectionScreen(mainViewModel = mainViewModel)
                    Screen.TextReading -> TextReadingScreen(mainViewModel = mainViewModel)
                    Screen.ImageDescription -> ImageDescriptionScreen(mainViewModel = mainViewModel)
                    Screen.PhotoCapture -> {
                        // Inject the platform-specific factory for the ViewModel
                        val photoCaptureViewModel = rememberPhotoCaptureViewModel(mainViewModel, photoCaptureViewModelFactory)
                        PhotoCaptureScreen(viewModel = photoCaptureViewModel)
                    }
                    Screen.History -> HistoryScreen(mainViewModel = mainViewModel)// NEW Screen
                    else -> {}
                }
            }
        }
    }
}


@Preview
@Composable
fun TaskAppAndroidPreview() {
    TaskApp()
}