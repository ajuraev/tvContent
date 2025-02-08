package com.example.tvcontent.ui.component

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.example.tvcontent.databinding.LayoutPlayerTextureBinding

@OptIn(UnstableApi::class)
@Composable
fun FullScreenVideo(
    url: String,
    playIndex: Int,
    onPlaybackEnded: () -> Unit,
    onError: (Exception) -> Unit = { Log.e("FullScreenVideo", "Error playing video", it) }
) {
    val context = LocalContext.current

    // 1) Create your cache-enabled DataSource.Factory
    val dataSourceFactory = remember {
        VideoCacheManager.getCacheDataSourceFactory(context)
    }
    val mediaSourceFactory = remember {
        DefaultMediaSourceFactory(dataSourceFactory)
    }

    // 2) Build the ExoPlayer once via remember { ... }
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            // Inject the media source factory that uses your cached DataSource
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF

                // 3) Attach a single listener
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        Log.d("FullScreen", "Playback state = $state")
                        if (state == Player.STATE_ENDED) {
                            onPlaybackEnded()
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        onError(error)
                        Log.e("FullScreen", "Player error: $error")
                    }

                    override fun onRenderedFirstFrame() {
                        Log.d("FullScreen", "Rendered first frame for $url!")
                    }
                })
            }
    }

    // 4) Whenever url or playIndex changes, just reset and prepare
    LaunchedEffect(url, playIndex) {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        exoPlayer.prepare()
    }

    // 5) Use the PlayerView with your composable
    AndroidViewBinding(
        factory = { inflater, parent, attachToParent ->
            LayoutPlayerTextureBinding.inflate(inflater, parent, attachToParent)
        },
        modifier = Modifier.fillMaxSize()
    ) {
        playerView.player = exoPlayer
        playerView.useController = false
    }

    // 6) Release the player when composable is removed
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
}

