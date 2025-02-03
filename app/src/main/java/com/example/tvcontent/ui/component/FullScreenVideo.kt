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
import androidx.media3.common.C
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

@OptIn(UnstableApi::class)
@Composable
fun FullScreenVideo(
    url: String,
    playIndex: Int,
    onPlaybackEnded: () -> Unit,
    onError: (Exception) -> Unit = { Log.e("FullScreenVideo", "Error playing video", it) }
) {
    val context = LocalContext.current

    // Create a stable reference to the ExoPlayer that survives recomposition
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setRenderersFactory(
                DefaultRenderersFactory(context)
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                    .setEnableDecoderFallback(true)
            )
            .setTrackSelector(
                DefaultTrackSelector(context).apply {
                    setParameters(buildUponParameters().setMaxVideoSizeSd())
                }
            )
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        DefaultLoadControl.DEFAULT_MIN_BUFFER_MS * 2,
                        DefaultLoadControl.DEFAULT_MAX_BUFFER_MS * 2,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS * 2,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS * 2
                    )
                    .build()
            )
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    VideoCacheManager.getCacheDataSourceFactory(context)
                )
            )
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
            }
    }

    // Set up the player listener using a side effect to handle changes to url/playIndex
    LaunchedEffect(url, playIndex) {
        exoPlayer.addListener(object : Player.Listener {
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

        // Reset and prepare the player with the new URL
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        exoPlayer.prepare()
    }

    // Create a stable reference to the PlayerView
    val playerView = remember {
        PlayerView(context).apply {
            useController = false
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFocusable = true
            isFocusableInTouchMode = true
            setKeepContentOnPlayerReset(true)  // Changed to true to maintain surface
            setShutterBackgroundColor(android.graphics.Color.BLACK)
        }
    }

    // Use LaunchedEffect to handle player assignment and surface preparation
    LaunchedEffect(playerView) {
        playerView.player = exoPlayer
    }

    AndroidView(
        factory = { playerView },
        modifier = Modifier.fillMaxSize()
    )

    // Clean up the player when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            try {
                exoPlayer.release()
            } catch (e: Exception) {
                Log.e("FullScreenVideo", "Error releasing player", e)
            }
        }
    }

}