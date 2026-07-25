package com.example.wearfilemanager.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ImageViewerScreen(
    file: File,
    onClose: () -> Unit
) {
    var bitmap by remember(file) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(file) { mutableStateOf(true) }

    LaunchedEffect(file) {
        isLoading = true
        bitmap = withContext(Dispatchers.IO) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                null
            }
        }
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (bitmap != null) {
            AndroidView(
                factory = { context ->
                    ImageCanvasView(context).apply {
                        setImageBitmap(bitmap)
                    }
                },
                update = { view ->
                    view.setImageBitmap(bitmap)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text("Failed to load image", color = Color.Red, fontSize = 12.sp)
        }

        // Top Exit / Back Button Overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        ) {
            Button(
                onClick = onClose,
                modifier = Modifier.size(30.dp),
                colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color(0xDDCC3333))
            ) {
                Text("✕", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}
