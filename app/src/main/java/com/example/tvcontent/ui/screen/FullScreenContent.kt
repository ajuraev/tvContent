package com.example.tvcontent.ui.screen

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.tv.material3.Text
import io.github.jan.supabase.SupabaseClient
import com.example.tvcontent.ui.component.FullScreenImage
import com.example.tvcontent.viewModel.ContentViewModel

@Composable
fun FullScreenContent(
    supabaseClient: SupabaseClient,
    contentViewModel: ContentViewModel,
    onBack: () -> Unit
) {
    // Intercept the TV remote back button
    BackHandler { onBack() }

    val contentList by contentViewModel.contentInserts.collectAsState()
    val latestItem = contentList.lastOrNull()

    if (latestItem != null) {
        val imageUrl = latestItem.first
        Log.d("FullScreenContent", "Displaying image URL: $imageUrl")
        FullScreenImage(url = imageUrl)
    } else {
        // Show a simple fallback when there is no content
        NoContentFallback()
    }
}

/**
 * A simple fallback composable to show when there's no content.
 */
@Composable
fun NoContentFallback() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No content available",
            color = Color.White,
            fontSize = 18.sp
        )
    }
}
