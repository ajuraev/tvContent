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

    // Observe the current playlist items
    val playlistItems by contentViewModel.currentPlaylistItemsFlow.collectAsState()

    // Handle empty playlist case
    if (playlistItems.isEmpty()) {
        NoContentFallback()
        return
    }

    var currentIndex by remember { mutableStateOf(0) }

    // Safely access the current item
    val currentItem = playlistItems.getOrNull(currentIndex % playlistItems.size)

    if (currentItem == null) {
        NoContentFallback()
        return
    }

    if (currentItem.is_video) {
        Log.d("Parent", "Showing item $currentIndex for URL=${currentItem.url}")
        // For a video, show it until playback completes
        FullScreenVideo(
            url = currentItem.url,
            playIndex = currentIndex,
            onPlaybackEnded = {
                // When the video finishes, go to the next item
                currentIndex += 1
            }
        )
    } else {
        // For an image, show it, then delay for duration
        FullScreenImage(url = currentItem.url)

        // Use a side-effect that waits duration, then advances to the next item
        LaunchedEffect(currentItem) {
            delay(currentItem.duration * 1000L)
            currentIndex += 1
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
