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
import com.example.task.navigation.Screen
import com.example.task.screen.NoiseTestScreen
import com.example.task.screen.TaskSelectionScreen
import com.example.task.screen.TextReadingScreen
import com.example.task.screen.ImageDescriptionScreen
import com.example.task.screen.PhotoCaptureScreen
import com.example.task.viewmodel.MainViewModel
import com.example.task.viewmodel.StartViewModel
import com.example.task.screen.StartScreen
import com.example.task.viewmodel.PhotoCaptureViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

// Bottom navigation imports
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import com.example.task.screen.HistoryScreen

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

        val currentScreen = uiState.currentScreen
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

        val onBack: () -> Unit = { mainViewModel.popBack() }
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
                            onClick = { mainViewModel.navigateTo(screen) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (currentScreen) {
                    Screen.Start -> StartScreen(viewModel = rememberStartViewModel(mainViewModel))
                    Screen.NoiseTest -> NoiseTestScreen(mainViewModel = mainViewModel)
                    Screen.TaskSelection -> TaskSelectionScreen(mainViewModel = mainViewModel)
                    Screen.TextReading -> TextReadingScreen(mainViewModel = mainViewModel)
                    Screen.ImageDescription -> ImageDescriptionScreen(mainViewModel = mainViewModel)
                    Screen.PhotoCapture -> {
                        val photoCaptureViewModel = rememberPhotoCaptureViewModel(mainViewModel, photoCaptureViewModelFactory)
                        PhotoCaptureScreen(viewModel = photoCaptureViewModel)
                    }
                    Screen.History -> HistoryScreen(mainViewModel = mainViewModel)
                    else -> {}
                }
            }
        }
    }
}