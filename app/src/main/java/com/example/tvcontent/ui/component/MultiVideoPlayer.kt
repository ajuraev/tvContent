package com.example.tvcontent.ui.component

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.tvcontent.data.model.PlaylistItem
import com.example.tvcontent.databinding.LayoutPlayerTextureBinding

@OptIn(UnstableApi::class)
@Composable
fun UnifiedMediaPlayer(
    playlistItems: List<PlaylistItem>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 1) Build a cache-enabled DataSource.Factory
    val dataSourceFactory = remember {
        VideoCacheManager.getCacheDataSourceFactory(context)
    }
    // 2) Create a DefaultMediaSourceFactory using the cache-enabled data source
    val mediaSourceFactory = remember {
        DefaultMediaSourceFactory(dataSourceFactory)
    }

    // 3) Build the ExoPlayer with that MediaSourceFactory
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ALL
            }
    }

    // Prepare the playlist whenever 'playlistItems' changes!
    LaunchedEffect(playlistItems) {
        exoPlayer.clearMediaItems()

        playlistItems.forEach { item ->
            val mediaItem = if (item.is_video) {
                MediaItem.fromUri(Uri.parse(item.url))
            } else {
                // Image item
                MediaItem.Builder()
                    .setUri(item.url)
                    .setImageDurationMs(item.duration * 1_000L)
                    .build()
            }
            exoPlayer.addMediaItem(mediaItem)
        }

        exoPlayer.prepare()
        exoPlayer.play()
    }

    // Compose <-> AndroidView bridge for PlayerView
    Box(modifier = Modifier
        .fillMaxSize()
        .focusRequester(FocusRequester())
        .focusable(false)
    ) {
        AndroidViewBinding(
            factory = { inflater, parent, attachToParent ->
                LayoutPlayerTextureBinding.inflate(inflater, parent, attachToParent).apply {
                    playerView.apply {
                        isFocusable = false
                        isFocusableInTouchMode = false
                    }
                }
            },
            modifier = modifier.fillMaxSize()
        ) {
            playerView.player = exoPlayer
            playerView.useController = false
            playerView.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
    }

    // Release player when removed
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
}
