package com.example.task

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.util.Log
import com.example.task.platform.AudioRecorder
import com.example.task.platform.CameraLauncher // <-- NEW IMPORT
import com.example.task.viewmodel.PhotoCaptureViewModel
import com.example.task.viewmodel.MainViewModel // <-- ADDED IMPORT

class MainActivity : ComponentActivity() {

    private val TAG = "NoiseTestApp_Activity"
    private var isPermissionGranted = mutableStateOf(false)

    // List of permissions required for the app
    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    // Store CameraLauncher instance managed by Activity lifecycle
    private lateinit var cameraLauncher: CameraLauncher

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            Log.i(TAG, "Permissions requested result. All granted: $allGranted")
            isPermissionGranted.value = allGranted
        }

    // NEW: Function to create the PhotoCaptureViewModel with Android dependencies
    @Composable
    private fun rememberPhotoCaptureViewModelAndroid(mainViewModel: MainViewModel): PhotoCaptureViewModel {
        return remember {
            PhotoCaptureViewModel(
                mainViewModel = mainViewModel,
                cameraLauncher = cameraLauncher // Inject the Activity-managed launcher
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate started.")

        // Initialize CameraLauncher early
        cameraLauncher = CameraLauncher(this) // Initialize with 'this' ComponentActivity

        // REQUIRED FIX: Initialize AudioRecorder with the application's cache directory
        AudioRecorder.Companion.initialize(cacheDir)

        // 1. Check if all permissions are already granted
        val allPermissionsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        when {
            allPermissionsGranted -> {
                isPermissionGranted.value = true
                Log.i(TAG, "All required permissions already GRANTED.")
            }
            // 2. Request permissions if not granted
            else -> {
                Log.w(TAG, "Permissions NOT fully granted. Requesting missing permissions.")
                requestPermissionLauncher.launch(requiredPermissions)
            }
        }

        // 3. Set content based on permission status
        setContent {
            val permissionGranted by remember { isPermissionGranted }
            Log.d(TAG, "Setting Compose Content. Permission granted: $permissionGranted")

            if (permissionGranted) {
                // Pass the custom ViewModel factory to the App component so it can correctly
                // instantiate the PhotoCaptureViewModel
                TaskApp(
                    photoCaptureViewModelFactory = { mainViewModel ->
                        rememberPhotoCaptureViewModelAndroid(mainViewModel)
                    }
                )
            } else {
                androidx.compose.material3.Text("Waiting for Microphone and Camera Permissions...")
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    TaskApp()
}