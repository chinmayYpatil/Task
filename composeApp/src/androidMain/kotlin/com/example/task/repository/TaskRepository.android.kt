package com.example.task.repository

import com.example.task.platform.AudioRecorder
import android.util.Log
import java.io.File
import java.io.IOException
import java.nio.charset.Charset

// ACTUAL: Android implementation of TaskStorage for file I/O
actual object TaskStorage {
    private const val FILENAME = "tasks.json"

    // Get the base directory from AudioRecorder's initialization
    private val taskFile: File?
        // FIX: Access companion object property correctly
        get() = AudioRecorder.Companion.baseDir?.let { File(it, FILENAME) }

    actual fun saveTasksJson(jsonString: String) {
        val file = taskFile
        if (file != null) {
            try {
                // FIX: Use writeText and default charset
                file.writeText(jsonString, Charset.defaultCharset())
                // FIX: Use .absolutePath on File object
                Log.i("TaskStorage", "Tasks successfully saved to: ${file.absolutePath}")
            } catch (e: IOException) {
                Log.e("TaskStorage", "Error writing tasks file: ${e.message}")
            }
        } else {
            Log.e("TaskStorage", "Base directory not available, cannot save tasks.")
        }
    }

    actual fun loadTasksJson(): String? {
        val file = taskFile
        return if (file != null && file.exists()) {
            try {
                // FIX: Use readText and default charset
                val content = file.readText(Charset.defaultCharset())
                Log.i("TaskStorage", "Tasks successfully loaded from file.")
                content
            } catch (e: IOException) {
                Log.e("TaskStorage", "Error reading tasks file: ${e.message}")
                null
            }
        } else {
            Log.w("TaskStorage", "Tasks file not found. Starting with empty list.")
            null
        }
    }
}