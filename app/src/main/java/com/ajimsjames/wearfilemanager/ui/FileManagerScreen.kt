package com.ajimsjames.wearfilemanager.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.material.Text
import com.ajimsjames.wearfilemanager.model.FileItem
import java.io.File
import java.util.Locale

sealed class ActiveViewer {
    object None : ActiveViewer()
    data class TextViewer(val file: File) : ActiveViewer()
    data class ImageViewer(val file: File) : ActiveViewer()
    data class AudioViewer(val file: File) : ActiveViewer()
}

data class CategoryStorageStats(
    val pdfCount: Int = 0,
    val pdfBytes: Long = 0,
    val imageCount: Int = 0,
    val imageBytes: Long = 0,
    val videoCount: Int = 0,
    val videoBytes: Long = 0,
    val audioCount: Int = 0,
    val audioBytes: Long = 0,
    val otherCount: Int = 0,
    val otherBytes: Long = 0
)

@Composable
fun FileManagerScreen() {
    val context = LocalContext.current
    var currentDir by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    var selectedFileItem by remember { mutableStateOf<FileItem?>(null) }
    var activeViewer by remember { mutableStateOf<ActiveViewer>(ActiveViewer.None) }
    var refreshKey by remember { mutableStateOf(0) }

    var showStorageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

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

    val categoryStats by remember(refreshKey) {
        mutableStateOf(calculateCategoryStats(Environment.getExternalStorageDirectory()))
    }

    fun openFile(fileItem: FileItem) {
        val file = fileItem.file
        val ext = file.extension.lowercase(Locale.US)

        when {
            ext == "pdf" -> {
                try {
                    val uri: Uri = FileProvider.getUriForFile(context, "com.ajimsjames.wearfilemanager.fileprovider", file)
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
            ext in listOf("mp3", "wav", "m4a", "ogg", "aac", "flac") -> {
                activeViewer = ActiveViewer.AudioViewer(file)
            }
            ext in listOf("txt", "log", "json", "csv", "xml", "html", "md", "py", "sh", "properties", "conf", "gradle", "kts") -> {
                activeViewer = ActiveViewer.TextViewer(file)
            }
            ext in listOf("png", "jpg", "jpeg", "bmp", "webp", "gif") -> {
                activeViewer = ActiveViewer.ImageViewer(file)
            }
            else -> {
                try {
                    val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                    val uri: Uri = FileProvider.getUriForFile(context, "com.ajimsjames.wearfilemanager.fileprovider", file)
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
                    contentPadding = PaddingValues(top = 52.dp, bottom = 24.dp)
                ) {
                    item {
                        Text(
                            text = "📁 Watch File Manager",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    // Directory Location Card
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
                                    text = "📍 " + (currentDir.absolutePath.replace("/storage/emulated/0", "Internal Storage")),
                                    color = Color(0xFFFFB300),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
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

                // Curved Bezel Top Navigation Bar
                CurvedLayout(
                    anchor = 270f,
                    modifier = Modifier.fillMaxSize()
                ) {
                    curvedComposable {
                        BezelPill("📁 Files", selected = true) { currentDir = Environment.getExternalStorageDirectory() }
                    }
                    curvedComposable {
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    curvedComposable {
                        BezelPill("📊 Storage", selected = false) { showStorageDialog = true }
                    }
                    curvedComposable {
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    curvedComposable {
                        BezelPill("⚙️ About", selected = false) { showAboutDialog = true }
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

                // Storage Info Modal Dialog with Detailed File Breakdown
                if (showStorageDialog) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xF0000000))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📊 Detailed Storage", color = Color(0xFFFFB300), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF333336))
                                        .clickable { showStorageDialog = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✕", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                item {
                                    Text("Internal Storage", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("${storageInfo.first} Used / ${storageInfo.second} Total", color = Color.LightGray, fontSize = 9.5.sp)
                                }

                                // 📄 PDF Category
                                item {
                                    StorageCategoryRow(
                                        icon = "📄",
                                        title = "PDF Documents",
                                        count = categoryStats.pdfCount,
                                        bytes = categoryStats.pdfBytes,
                                        color = Color(0xFFD50000)
                                    )
                                }

                                // 🖼️ Images / Pics Category
                                item {
                                    StorageCategoryRow(
                                        icon = "🖼️",
                                        title = "Images & Photos",
                                        count = categoryStats.imageCount,
                                        bytes = categoryStats.imageBytes,
                                        color = Color(0xFF0288D1)
                                    )
                                }

                                // 🎵 Audio / Music Category
                                item {
                                    StorageCategoryRow(
                                        icon = "🎵",
                                        title = "Audio & Music",
                                        count = categoryStats.audioCount,
                                        bytes = categoryStats.audioBytes,
                                        color = Color(0xFF00E676)
                                    )
                                }

                                // 🎥 Videos Category
                                item {
                                    StorageCategoryRow(
                                        icon = "🎥",
                                        title = "Videos & Clips",
                                        count = categoryStats.videoCount,
                                        bytes = categoryStats.videoBytes,
                                        color = Color(0xFFFF9800)
                                    )
                                }

                                // 📁 Other Files Category
                                item {
                                    StorageCategoryRow(
                                        icon = "📁",
                                        title = "Other & System Files",
                                        count = categoryStats.otherCount,
                                        bytes = categoryStats.otherBytes,
                                        color = Color(0xFFAB47BC)
                                    )
                                }
                            }
                        }
                    }
                }

                // About App Modal Dialog
                if (showAboutDialog) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xF0000000))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚙️ About App", color = Color(0xFFFFB300), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF333336))
                                        .clickable { showAboutDialog = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✕", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item {
                                    Text("📁 Wear OS File Manager v1.2.0", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("By Aju George", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(bottom = 6.dp))
                                }
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF2C2C2E))
                                            .padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("• Detailed PDF, Pic, Vid, Music Breakdown", color = Color.LightGray, fontSize = 9.sp)
                                        Text("• Native Text, Image & Audio Viewers", color = Color.LightGray, fontSize = 9.sp)
                                        Text("• Direct PDF Viewer Integration", color = Color.LightGray, fontSize = 9.sp)
                                        Text("• Target: Samsung Galaxy Watch 6", color = Color(0xFFFFB300), fontSize = 9.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun StorageCategoryRow(icon: String, title: String, count: Int, bytes: Long, color: Color) {
    val formattedSize = formatByteSize(bytes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2C2C2E))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(icon, fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
            Column {
                Text(title, color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                Text("$count files", color = Color.Gray, fontSize = 8.sp)
            }
        }
        Text(formattedSize, color = color, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BezelPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFFFFB300) else Color(0xFF2C2C2E))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.Black else Color.Gray,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatByteSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb > 1024) String.format(Locale.US, "%.1f GB", mb / 1024.0) else String.format(Locale.US, "%.1f MB", mb)
}

private fun calculateCategoryStats(root: File): CategoryStorageStats {
    var pdfC = 0; var pdfB = 0L
    var imgC = 0; var imgB = 0L
    var vidC = 0; var vidB = 0L
    var audC = 0; var audB = 0L
    var othC = 0; var othB = 0L

    fun scan(dir: File, depth: Int) {
        if (depth > 3) return
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.name.startsWith(".")) continue
            if (f.isDirectory) {
                scan(f, depth + 1)
            } else {
                val len = f.length()
                val ext = f.extension.lowercase(Locale.US)
                when {
                    ext == "pdf" -> { pdfC++; pdfB += len }
                    ext in listOf("png", "jpg", "jpeg", "webp", "bmp", "gif") -> { imgC++; imgB += len }
                    ext in listOf("mp4", "mkv", "webm", "avi", "3gp", "mov") -> { vidC++; vidB += len }
                    ext in listOf("mp3", "wav", "m4a", "ogg", "aac", "flac") -> { audC++; audB += len }
                    else -> { othC++; othB += len }
                }
            }
        }
    }

    try {
        scan(root, 0)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return CategoryStorageStats(
        pdfCount = pdfC, pdfBytes = pdfB,
        imageCount = imgC, imageBytes = imgB,
        videoCount = vidC, videoBytes = vidB,
        audioCount = audC, audioBytes = audB,
        otherCount = othC, otherBytes = othB
    )
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
