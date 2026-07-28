package com.ajimsjames.wearfilemanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import java.io.File

@Composable
fun TextViewerScreen(
    file: File,
    onClose: () -> Unit
) {
    var content by remember(file) { mutableStateOf("Loading...") }

    LaunchedEffect(file) {
        try {
            content = file.readText(Charsets.UTF_8).take(8000)
            if (content.isEmpty()) content = "(Empty File)"
        } catch (e: Exception) {
            content = "Error reading file: ${e.localizedMessage}"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 36.dp, bottom = 24.dp)
        ) {
            item {
                Text(
                    text = file.name,
                    color = Color(0xFF81D4FA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(10.dp)
                ) {
                    Text(
                        text = content,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Top Exit / Back Button Overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
        ) {
            Button(
                onClick = onClose,
                modifier = Modifier.size(28.dp),
                colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color(0xDDCC3333))
            ) {
                Text("✕", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}
