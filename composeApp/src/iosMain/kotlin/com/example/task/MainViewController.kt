package com.example.task

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.remember
import com.example.task.viewmodel.MainViewModel
import com.example.task.viewmodel.PhotoCaptureViewModel

fun MainViewController() = ComposeUIViewController {
    // FIX: Use the @Composable lambda syntax for the factories
    TaskApp(
        mainViewModelFactory = {
            remember { MainViewModel() }
        },
        photoCaptureViewModelFactory = { mainVm ->
            remember { PhotoCaptureViewModel(mainVm) }
        }
    )
}
