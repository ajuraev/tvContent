package com.example.tvcontent.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.tv.material3.Text
import com.example.tvcontent.data.model.PlaylistItem
import com.example.tvcontent.ui.component.UnifiedMediaPlayer
import com.example.tvcontent.viewModel.ContentViewModel

@Composable
fun FullScreenPlayer(
    contentViewModel: ContentViewModel,
    onBack: () -> Unit
) {
    // Handle the system back press
    BackHandler { onBack() }

    // Collect your playlist items from the ViewModel
    val playlistItems by contentViewModel.currentPlaylistItemsFlow.collectAsState()

    if (playlistItems.isEmpty()) {
        // Show a fallback message if no items
        NoContentFallback()
    } else {
        // Use the UnifiedMediaPlayer to handle both videos and images in one playlist
        UnifiedMediaPlayer(
            playlistItems = playlistItems,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun NoContentFallback() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "No content available")
    }
}
