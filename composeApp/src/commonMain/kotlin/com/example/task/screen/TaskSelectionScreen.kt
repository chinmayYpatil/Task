package com.example.task.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.task.navigation.Screen
import com.example.task.viewmodel.MainViewModel
import com.example.task.viewmodel.TaskSelectionViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun rememberTaskSelectionViewModel(mainViewModel: MainViewModel): TaskSelectionViewModel {
    return remember { TaskSelectionViewModel(mainViewModel) }
}

@Composable
fun TaskSelectionScreen(mainViewModel: MainViewModel) {
    val viewModel = rememberTaskSelectionViewModel(mainViewModel)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp), // Removed safeContentPadding
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Select a Recording Task",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // 1. Text Reading Task
        TaskSelectionButton(
            title = "1. Text Reading Task",
            onClick = { viewModel.onTaskSelected(Screen.TextReading) }
        )

        Spacer(Modifier.height(16.dp))

        // 2. Image Description Task
        TaskSelectionButton(
            title = "2. Image Description Task",
            onClick = { viewModel.onTaskSelected(Screen.ImageDescription) }
        )

        Spacer(Modifier.height(16.dp))

        // 3. Photo Capture Task
        TaskSelectionButton(
            title = "3. Photo Capture Task",
            onClick = { viewModel.onTaskSelected(Screen.PhotoCapture) } // <-- UPDATED NAV TARGET
        )

        Spacer(Modifier.weight(1f))

        // Navigation back is simple, but handled by ViewModel
        Button(onClick = viewModel::onBackClick) {
            Text("Back to Noise Test")
        }
    }
}

@Composable
private fun TaskSelectionButton(title: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(60.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Preview
@Composable
fun TaskSelectionScreenPreview() {
    MaterialTheme {
        // Note: Preview needs a mock MainViewModel
        TaskSelectionScreen(mainViewModel = MainViewModel())
    }
}