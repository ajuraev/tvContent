package com.example.tvcontent.ui.screen

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.example.tvcontent.ui.component.FullScreenImage
import com.example.tvcontent.ui.component.FullScreenVideo
import com.example.tvcontent.viewModel.ContentViewModel
import kotlinx.coroutines.delay

@Composable
fun FullScreenPlayer(
    contentViewModel: ContentViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val playlistItems by contentViewModel.currentPlaylistItemsFlow.collectAsState()

    if (playlistItems.isEmpty()) {
        NoContentFallback()
        return
    }

    var currentIndex by remember { mutableStateOf(0) }
    val currentItem = playlistItems.getOrNull(currentIndex % playlistItems.size) ?: run {
        NoContentFallback()
        return
    }

    // Keep screen on during playback
    Box(modifier = Modifier
        .fillMaxSize()
    ) {
        if (currentItem.is_video) {
            FullScreenVideo(
                url = currentItem.url,
                playIndex = currentIndex,
                onPlaybackEnded = { currentIndex += 1 }
            )
        } else {
            FullScreenImage(url = currentItem.url)
            LaunchedEffect(currentItem) {
                delay(currentItem.duration * 1000L)
                currentIndex += 1
            }
        }
    }
}


@Composable
fun NoContentFallback() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No content available",
            fontSize = 18.sp
        )
    }
}
