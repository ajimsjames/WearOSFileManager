package com.example.wearfilemanager.ui

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.wear.compose.material.Text
import java.io.File

@Composable
fun AudioPlayerScreen(
    file: File,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(file) {
        val player = MediaPlayer().apply {
            setDataSource(context, Uri.fromFile(file))
            prepare()
            start()
        }
        isPlaying = true
        mediaPlayer = player

        player.setOnCompletionListener {
            isPlaying = false
        }

        onDispose {
            player.stop()
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1565C0)),
                contentAlignment = Alignment.Center
            ) {
                Text("🎵", fontSize = 26.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = file.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Play / Pause Toggle Button
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) Color(0xFFFF9800) else Color(0xFF00E676))
                        .clickable {
                            mediaPlayer?.let { player ->
                                if (player.isPlaying) {
                                    player.pause()
                                    isPlaying = false
                                } else {
                                    player.start()
                                    isPlaying = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isPlaying) "⏸" else "▶", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD32F2F))
                        .clickable {
                            mediaPlayer?.stop()
                            onClose()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏹", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2C2C2E))
                    .clickable { onClose() }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Close Audio Player", color = Color.LightGray, fontSize = 10.sp)
            }
        }
    }
}
