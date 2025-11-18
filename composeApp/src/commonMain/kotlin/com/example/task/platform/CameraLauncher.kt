package com.example.task.platform

import kotlinx.coroutines.flow.SharedFlow

/**
 * Expected interface for launching the device camera and handling the result.
 */
expect class CameraLauncher {

    // Launches the camera app to capture an image.
    fun launch()

    // Flow to emit the path of the captured image file after successful capture.
    val capturedImagePath: SharedFlow<String>
}

expect fun getCameraLauncher(): CameraLauncher