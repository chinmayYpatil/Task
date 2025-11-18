package com.example.task.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File
import java.io.IOException
import kotlin.time.Clock

private const val TAG = "CameraLauncher_Android"
private const val FILE_PROVIDER_AUTHORITY = "com.example.task.fileprovider"

actual class CameraLauncher(private val activity: ComponentActivity) {

    // SharedFlow is used to bridge the one-shot ActivityResult callback to the ViewModel's flow collector
    private val _capturedImagePath = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    actual val capturedImagePath: SharedFlow<String> = _capturedImagePath

    private var currentPhotoPath: String? = null

    private val cameraLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Photo captured successfully. The image is saved to currentPhotoPath.
                currentPhotoPath?.let { path ->
                    _capturedImagePath.tryEmit(path)
                }
            } else {
                Log.d(TAG, "Camera capture cancelled or failed. Result code: ${result.resultCode}")
                currentPhotoPath = null
            }
        }

    actual fun launch() {
        val context = activity.applicationContext
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(context.packageManager) != null) {
            val photoFile: File? = try {
                createImageFile(context)
            } catch (ex: IOException) {
                Log.e(TAG, "Error creating image file: ${ex.message}")
                null
            }

            photoFile?.also {
                currentPhotoPath = it.absolutePath
                val photoURI: Uri = FileProvider.getUriForFile(
                    context,
                    FILE_PROVIDER_AUTHORITY,
                    it
                )
                // Grant URI permission to the camera app
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                cameraLauncher.launch(takePictureIntent)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(context: Context): File {
        // Create an image file name
        @kotlin.OptIn(kotlin.time.ExperimentalTime::class)
        val imageFileName = "JPEG_${Clock.System.now().toEpochMilliseconds()}_"
        // Use external cache storage where the camera app has permission to write.
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }
}

actual fun getCameraLauncher(): CameraLauncher {
    // This function can no longer be used directly by the ViewModel in `commonMain` since 
    // it requires an Android Context/Activity. We rely on dependency injection from MainActivity.
    throw IllegalStateException("CameraLauncher must be instantiated by MainActivity and injected.")
}