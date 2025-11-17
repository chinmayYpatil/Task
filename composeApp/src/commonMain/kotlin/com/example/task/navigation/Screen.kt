package com.example.task.navigation

// Sealed interface for all possible screens in the app
sealed interface Screen {
    data object Start : Screen
    data object NoiseTest : Screen
    data object TaskSelection : Screen
    data object TextReading : Screen
    data object ImageDescription : Screen // <-- ADDED SCREEN
    data object PhotoCapture : Screen
    data object History : Screen
}