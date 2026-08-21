package com.ajugeorge.wearfilemanager.ui

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
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.material.Text

import com.ajugeorge.wearfilemanager.model.FileItem
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
    val fileItems: List<FileItem> = remember(currentDir, refreshKey) {
        currentDir.listFiles()
            ?.filter { !it.name.startsWith(".") }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.US) }))
            ?.map { FileItem(it) }
            ?: emptyList()
    }

    val storageInfo = remember(refreshKey) {
        getStorageStats()
    }


    fun openFile(fileItem: FileItem) {
        val file = fileItem.file
        val ext = file.extension.lowercase(Locale.US)

        when {
            // APK -> Direct Android Package Installer Trigger
            ext == "apk" -> {
                try {
                    val uri: Uri = FileProvider.getUriForFile(context, "com.ajugeorge.wearfilemanager.fileprovider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Toast.makeText(context, "Opening Package Installer...", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "APK Install Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
            // PDF -> Open Wear PDF Reader directly with URI
            ext == "pdf" -> {
                try {
                    val uri: Uri = FileProvider.getUriForFile(context, "com.ajugeorge.wearfilemanager.fileprovider", file)
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
                    val uri: Uri = FileProvider.getUriForFile(context, "com.ajugeorge.wearfilemanager.fileprovider", file)
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

    var showAboutDialog by remember { mutableStateOf(false) }

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
                    .background(Color(0xFF0D0E11))
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(top = 40.dp, bottom = 44.dp, start = 10.dp, end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Storage Indicator Card
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF16181D))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "💾 Storage: ${storageInfo.first} / ${storageInfo.second}",
                                color = Color(0xFF00E5FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentDir.name.ifEmpty { "Internal Storage" },
                                color = Color.Gray,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Up Directory button
                    if (currentDir.parentFile != null && currentDir.absolutePath != "/storage/emulated/0" && currentDir.absolutePath != "/sdcard") {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF21242D))
                                    .clickable { currentDir = currentDir.parentFile!! }
                                    .padding(vertical = 6.dp, horizontal = 10.dp)
                            ) {
                                Text("📁  .. (Parent Folder)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (fileItems.isEmpty()) {
                        item {
                            Text(
                                text = "Empty Directory",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    } else {
                        items(fileItems) { item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (item.isDirectory) Color(0xFF16181D) else Color(0xFF1A1D24))
                                    .clickable {
                                        if (item.isDirectory) {
                                            currentDir = item.file
                                        } else {
                                            selectedFileItem = item
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = item.fileTypeIcon,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${item.formattedSize} • ${item.formattedDate}",
                                            color = Color.Gray,
                                            fontSize = 8.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Top Curved Bezel Title / Status
                CurvedLayout(
                    anchor = 270f,
                    anchorType = androidx.wear.compose.foundation.AnchorType.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    curvedComposable {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xEE16181D))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text("📁 File Explorer", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Bottom Curved Bezel Actions Pill
                CurvedLayout(
                    anchor = 90f,
                    anchorType = androidx.wear.compose.foundation.AnchorType.Center,
                    angularDirection = androidx.wear.compose.foundation.CurvedDirection.Angular.CounterClockwise,
                    modifier = Modifier.fillMaxSize()
                ) {

                    curvedComposable {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xEE21242D))
                                .clickable { refreshKey++ }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("🔄 Refresh", color = Color(0xFF00E5FF), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    curvedComposable {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    curvedComposable {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xEE21242D))
                                .clickable { showAboutDialog = true }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("ℹ️ About", color = Color(0xFFFFAB00), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }



                // File Action Modal Dialog
                selectedFileItem?.let { fileItem ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xF0000000))
                            .clickable { selectedFileItem = null }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF16181D))
                                .padding(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = fileItem.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${fileItem.formattedSize} • ${fileItem.formattedDate}",
                                    color = Color.Gray,
                                    fontSize = 9.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Open / Install Button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (fileItem.file.extension.equals("apk", ignoreCase = true)) Color(0xFF00E676) else Color(0xFF00E5FF))
                                        .clickable {
                                            val itemToOpen = fileItem
                                            selectedFileItem = null
                                            openFile(itemToOpen)
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (fileItem.file.extension.equals("apk", ignoreCase = true)) "📦 Install APK" else "👁️ Open File",
                                        color = Color.Black,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 4.dp)
                                            .clip(RoundedCornerShape(10.dp))
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
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🗑️ Delete", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 4.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF21242D))
                                            .clickable { selectedFileItem = null }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Close", color = Color.White, fontSize = 9.5.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // About Modal Dialog
                if (showAboutDialog) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xF0000000))
                            .clickable { showAboutDialog = false }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF16181D))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📁 WearOSFileManager", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Version 2.4.0 (Code 4)", color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Changelog v2.4.0:", color = Color(0xFFFFAB00), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            Text("• Material 3 OLED Circular Bezel HUD", color = Color.LightGray, fontSize = 8.sp)
                            Text("• Direct APK Package Sideload Installer", color = Color.LightGray, fontSize = 8.sp)
                            Text("• Fast Storage Telemetry & Media Viewer", color = Color.LightGray, fontSize = 8.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF21242D))
                                    .clickable { showAboutDialog = false }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Dismiss", color = Color.White, fontSize = 9.sp)
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

