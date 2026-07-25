package com.example.wearfilemanager.ui

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.wear.compose.material.Text
import com.example.wearfilemanager.model.FileItem
import java.io.File
import java.util.Locale

sealed class ActiveViewer {
    object None : ActiveViewer()
    data class TextViewer(val file: File) : ActiveViewer()
    data class ImageViewer(val file: File) : ActiveViewer()
    data class AudioViewer(val file: File) : ActiveViewer()
}

@Composable
fun FileManagerScreen() {
    val context = LocalContext.current
    var currentDir by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    var selectedFileItem by remember { mutableStateOf<FileItem?>(null) }
    var activeViewer by remember { mutableStateOf<ActiveViewer>(ActiveViewer.None) }
    var refreshKey by remember { mutableStateOf(0) }

    // Read Directory items
    val fileItems by remember(currentDir, refreshKey) {
        mutableStateOf(
            currentDir.listFiles()
                ?.filter { !it.name.startsWith(".") }
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?.map { FileItem(it) }
                ?: emptyList()
        )
    }

    val storageInfo by remember(refreshKey) {
        mutableStateOf(getStorageStats())
    }

    fun openFile(fileItem: FileItem) {
        val file = fileItem.file
        val ext = file.extension.lowercase(Locale.US)

        when {
            // PDF -> Open Wear PDF Reader directly with URI
            ext == "pdf" -> {
                try {
                    val uri: Uri = FileProvider.getUriForFile(context, "com.example.wearfilemanager.fileprovider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val uri = Uri.fromFile(file)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (ex: Exception) {
                        Toast.makeText(context, "Failed to open PDF: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            // Audio Files -> Native Audio Player
            ext in listOf("mp3", "wav", "m4a", "ogg", "aac", "flac") -> {
                activeViewer = ActiveViewer.AudioViewer(file)
            }
            // Text / Log / Code Files -> Open Native Text Viewer
            ext in listOf("txt", "log", "json", "csv", "xml", "html", "md", "py", "sh", "properties", "conf", "gradle", "kts") -> {
                activeViewer = ActiveViewer.TextViewer(file)
            }
            // Images -> Open Native Image Viewer
            ext in listOf("png", "jpg", "jpeg", "bmp", "webp", "gif") -> {
                activeViewer = ActiveViewer.ImageViewer(file)
            }
            // Other file types -> System FileProvider Intent
            else -> {
                try {
                    val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                    val uri: Uri = FileProvider.getUriForFile(context, "com.example.wearfilemanager.fileprovider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mime)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No app found to handle ${file.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    when (val viewer = activeViewer) {
        is ActiveViewer.TextViewer -> {
            TextViewerScreen(file = viewer.file, onClose = { activeViewer = ActiveViewer.None })
        }
        is ActiveViewer.ImageViewer -> {
            ImageViewerScreen(file = viewer.file, onClose = { activeViewer = ActiveViewer.None })
        }
        is ActiveViewer.AudioViewer -> {
            AudioPlayerScreen(file = viewer.file, onClose = { activeViewer = ActiveViewer.None })
        }
        is ActiveViewer.None -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(horizontal = 8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(vertical = 24.dp)
                ) {
                    item {
                        Text(
                            text = "Watch File Manager",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    // Storage Indicator Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Storage: ${storageInfo.first} / ${storageInfo.second}",
                                    color = Color(0xFF81D4FA),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentDir.name.ifEmpty { "/sdcard" },
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Up Directory button
                    if (currentDir.parentFile != null && currentDir.absolutePath != "/storage/emulated/0" && currentDir.absolutePath != "/sdcard") {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF2C2C2E))
                                    .clickable { currentDir = currentDir.parentFile!! }
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                            ) {
                                Text("📁  .. (Parent Folder)", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    if (fileItems.isEmpty()) {
                        item {
                            Text(
                                text = "Empty Directory",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 20.dp)
                            )
                        }
                    } else {
                        items(fileItems) { item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (item.isDirectory) Color(0xFF1E2A38) else Color(0xFF1C1C1E))
                                    .clickable {
                                        if (item.isDirectory) {
                                            currentDir = item.file
                                        } else {
                                            selectedFileItem = item
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = item.fileTypeIcon,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${item.formattedSize} • ${item.formattedDate}",
                                            color = Color.Gray,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(top = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // File Action Modal Dialog
                selectedFileItem?.let { fileItem ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFEE000000))
                            .clickable { selectedFileItem = null }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF222224))
                                .padding(14.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = fileItem.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "${fileItem.formattedSize} • ${fileItem.formattedDate}",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Open Button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1565C0))
                                        .clickable {
                                            val itemToOpen = fileItem
                                            selectedFileItem = null
                                            openFile(itemToOpen)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("👁️ Open File", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    // Delete Button
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFD32F2F))
                                            .clickable {
                                                if (fileItem.file.delete()) {
                                                    Toast.makeText(context, "Deleted ${fileItem.name}", Toast.LENGTH_SHORT).show()
                                                    selectedFileItem = null
                                                    refreshKey++
                                                } else {
                                                    Toast.makeText(context, "Failed to delete file", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🗑️ Delete", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Close Button
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF444446))
                                            .clickable { selectedFileItem = null }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Close", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getStorageStats(): Pair<String, String> {
    return try {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val availableBytes = availableBlocks * blockSize
        val usedBytes = totalBytes - availableBytes

        val usedMb = usedBytes / (1024.0 * 1024.0)
        val totalMb = totalBytes / (1024.0 * 1024.0)

        val usedStr = if (usedMb > 1024) String.format(Locale.US, "%.1f GB", usedMb / 1024.0) else String.format(Locale.US, "%.0f MB", usedMb)
        val totalStr = if (totalMb > 1024) String.format(Locale.US, "%.1f GB", totalMb / 1024.0) else String.format(Locale.US, "%.0f MB", totalMb)

        Pair(usedStr, totalStr)
    } catch (e: Exception) {
        Pair("N/A", "N/A")
    }
}
