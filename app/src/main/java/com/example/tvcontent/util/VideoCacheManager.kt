@file:UnstableApi

package com.example.tvcontent.ui.component

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import java.io.File

object VideoCacheManager {
    private var cache: Cache? = null

    @Synchronized
    fun getCache(context: Context): Cache {
        if (cache == null) {
            val cacheSize = 500L * 1024L * 1024L // 500MB for TV
            val cacheDir = File(context.cacheDir, "exoplayer_video_cache")
            val databaseProvider = StandaloneDatabaseProvider(context)

            cache = SimpleCache(
                cacheDir,
                LeastRecentlyUsedCacheEvictor(cacheSize),
                databaseProvider
            )
        }
        return cache!!
    }

    fun getCacheDataSourceFactory(context: Context): DataSource.Factory {
        val upstreamFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15000) // Longer timeout for TV
            .setReadTimeoutMs(15000)
            .setAllowCrossProtocolRedirects(true)

        return CacheDataSource.Factory()
            .setCache(getCache(context))
            .setUpstreamDataSourceFactory(upstreamFactory)
//            .setCacheWriteDataSinkFactory(null) // Disable writing while playing
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}

