package com.example.wearfilemanager.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isDirectory) 0L else file.length(),
    val lastModified: Long = file.lastModified()
) {
    val formattedSize: String
        get() {
            if (isDirectory) return "Folder"
            val kb = size / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
                else -> "$size B"
            }
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
            return sdf.format(Date(lastModified))
        }

    val fileTypeIcon: String
        get() = when {
            isDirectory -> "📁"
            name.endsWith(".pdf", ignoreCase = true) -> "📄"
            name.endsWith(".png", ignoreCase = true) || name.endsWith(".jpg", ignoreCase = true) -> "🖼️"
            name.endsWith(".mp3", ignoreCase = true) || name.endsWith(".wav", ignoreCase = true) -> "🎵"
            name.endsWith(".txt", ignoreCase = true) || name.endsWith(".log", ignoreCase = true) -> "📝"
            name.endsWith(".apk", ignoreCase = true) -> "📦"
            else -> "📄"
        }
}
