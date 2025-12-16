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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.task.navigation.Screen
import com.example.task.screen.NoiseTestScreen
import com.example.task.screen.TaskSelectionScreen
import com.example.task.screen.TextReadingScreen
import com.example.task.screen.ImageDescriptionScreen
import com.example.task.screen.PhotoCaptureScreen
import com.example.task.screen.StartScreen
import com.example.task.screen.HistoryScreen
import com.example.task.viewmodel.MainViewModel
import com.example.task.viewmodel.StartViewModel
import com.example.task.viewmodel.ImageDescriptionViewModel
import com.example.task.viewmodel.PhotoCaptureViewModel
import com.example.task.viewmodel.TaskHistoryViewModel
import com.example.task.viewmodel.TextReadingViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.LaunchedEffect

// Type aliases for ViewModel factories to support platform-specific injection
typealias MainViewModelFactory = @Composable () -> MainViewModel
typealias PhotoCaptureViewModelFactory = @Composable (MainViewModel) -> PhotoCaptureViewModel

@Composable
fun rememberMainViewModel(factory: MainViewModelFactory?): MainViewModel {
    // Uses the provided factory (for iOS) or manually instantiates for other platforms
    return factory?.invoke() ?: remember { MainViewModel() }
}

@Composable
fun rememberStartViewModel(mainViewModel: MainViewModel): StartViewModel {
    return remember { StartViewModel(mainViewModel) }
}
// NEW: Standard factories for other ViewModels (used for cleanup logic)
@Composable
fun rememberTextReadingViewModel(mainViewModel: MainViewModel): TextReadingViewModel {
    return remember { TextReadingViewModel(mainViewModel) }
}
@Composable
fun rememberImageDescriptionViewModel(mainViewModel: MainViewModel): ImageDescriptionViewModel {
    return remember { ImageDescriptionViewModel(mainViewModel = mainViewModel) }
}
@Composable
fun rememberHistoryViewModel(): TaskHistoryViewModel {
    return remember { TaskHistoryViewModel() }
}


@Composable
fun rememberPhotoCaptureViewModel(mainViewModel: MainViewModel, factory: PhotoCaptureViewModelFactory?): PhotoCaptureViewModel {
    return factory?.invoke(mainViewModel) ?: remember { PhotoCaptureViewModel(mainViewModel) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskApp(
    mainViewModelFactory: MainViewModelFactory? = null,
    photoCaptureViewModelFactory: PhotoCaptureViewModelFactory? = null
) {
    MaterialTheme {
        val mainViewModel = rememberMainViewModel(mainViewModelFactory)
        val uiState by mainViewModel.uiState.collectAsState()

        // Remember ViewModels that require explicit cleanup on navigation
        val textReadingViewModel = rememberTextReadingViewModel(mainViewModel)
        val imageDescriptionViewModel = rememberImageDescriptionViewModel(mainViewModel)
        val photoCaptureViewModel = rememberPhotoCaptureViewModel(mainViewModel, photoCaptureViewModelFactory)
        val historyViewModel = rememberHistoryViewModel()

        val currentScreen = uiState.currentScreen

        // NEW: LaunchedEffect to call startNewTask() whenever a task screen is entered.
        LaunchedEffect(currentScreen) {
            when (currentScreen) {
                Screen.TextReading -> textReadingViewModel.startNewTask()
                Screen.ImageDescription -> imageDescriptionViewModel.startNewTask()
                Screen.PhotoCapture -> photoCaptureViewModel.startNewTask()
                Screen.History -> historyViewModel.loadTasks() // Refresh history when entering
                else -> {}
            }
        }

        val title = when (currentScreen) {
            Screen.Start -> "Sample Task App"
            Screen.NoiseTest -> "Noise Test"
            Screen.TaskSelection -> "Select Task"
            Screen.TextReading -> "Text Reading Task"
            Screen.ImageDescription -> "Image Description Task"
            Screen.PhotoCapture -> "Photo Capture Task"
            Screen.History -> "Task History"
            else -> "App"
        }

        val onBack: () -> Unit = {
            // FIX: Stop audio playback on the active screen's ViewModel before navigating back
            when (currentScreen) {
                Screen.TextReading -> textReadingViewModel.stopPlayback()
                Screen.ImageDescription -> imageDescriptionViewModel.stopPlayback()
                Screen.PhotoCapture -> photoCaptureViewModel.stopPlayback()
                Screen.History -> historyViewModel.stopPlayback()
                else -> {}
            }
            mainViewModel.popBack()
        }

        val canNavigateBack = currentScreen != Screen.Start && currentScreen != Screen.History
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
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        Screen.Start -> Icons.Filled.Home
                                        Screen.History -> Icons.Filled.History
                                        else -> Icons.Filled.Home
                                    },
                                    contentDescription = null
                                )
                            },
                            label = { Text(if (screen == Screen.Start) "Home" else "History") },
                            selected = currentScreen == screen,
                            onClick = {
                                // FIX: Stop audio playback on the active screen's ViewModel before navigating
                                when (currentScreen) {
                                    Screen.TextReading -> textReadingViewModel.stopPlayback()
                                    Screen.ImageDescription -> imageDescriptionViewModel.stopPlayback()
                                    Screen.PhotoCapture -> photoCaptureViewModel.stopPlayback()
                                    Screen.History -> historyViewModel.stopPlayback()
                                    else -> {}
                                }
                                mainViewModel.navigateTo(screen)
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                // Pass the remembered ViewModel instances to the screens
                when (currentScreen) {
                    Screen.Start -> StartScreen(viewModel = rememberStartViewModel(mainViewModel))
                    Screen.NoiseTest -> NoiseTestScreen(mainViewModel = mainViewModel)
                    Screen.TaskSelection -> TaskSelectionScreen(mainViewModel = mainViewModel)
                    Screen.TextReading -> TextReadingScreen(viewModel = textReadingViewModel) // FIX: Uses 'viewModel' param
                    Screen.ImageDescription -> ImageDescriptionScreen(viewModel = imageDescriptionViewModel) // FIX: Uses 'viewModel' param
                    Screen.PhotoCapture -> PhotoCaptureScreen(viewModel = photoCaptureViewModel)
                    Screen.History -> HistoryScreen(viewModel = historyViewModel) // FIX: Uses 'viewModel' param
                    else -> {}
                }
            }
        }
    }
}