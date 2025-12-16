package com.example.task.repository

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*

actual object TaskStorage {
    private const val FILENAME = "tasks.json"

    private fun getFileUrl(): NSURL? {
        val fileManager = NSFileManager.defaultManager
        val urls = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
        val documentDirectory = urls.last() as? NSURL
        return documentDirectory?.URLByAppendingPathComponent(FILENAME)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun saveTasksJson(jsonString: String) {
        val url = getFileUrl() ?: return
        val nsString = jsonString as NSString
        nsString.writeToURL(url, true, NSUTF8StringEncoding, null)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun loadTasksJson(): String? {
        val url = getFileUrl() ?: return null
        if (!NSFileManager.defaultManager.fileExistsAtPath(url.path!!)) return null

        return try {
            NSString.stringWithContentsOfURL(url, NSUTF8StringEncoding, null) as String
        } catch (e: Exception) {
            null
        }
    }
}