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
import com.example.task.platform.DecibelMeter
import android.util.Log // <--- New Import

class MainActivity : ComponentActivity() {

    private val TAG = "NoiseTestApp_Activity" // <--- New Tag
    private var isPermissionGranted = mutableStateOf(false)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            Log.i(TAG, "Permission requested result: $isGranted")
            if (isGranted) {
                isPermissionGranted.value = true
            } else {
                isPermissionGranted.value = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate started.")

        // 1. Check if permission is already granted
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                isPermissionGranted.value = true
                Log.i(TAG, "Permission already GRANTED.")
            }
            // 2. Request permission if not granted
            else -> {
                Log.w(TAG, "Permission NOT granted. Requesting permission.")
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        // 3. Set content based on permission status
        setContent {
            val permissionGranted by remember { isPermissionGranted }
            Log.d(TAG, "Setting Compose Content. Permission granted: $permissionGranted")

            if (permissionGranted) {
                TaskApp()
            } else {
                androidx.compose.material3.Text("Waiting for Microphone Permission...")
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    TaskApp()
}