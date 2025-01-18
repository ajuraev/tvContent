package com.example.tvcontent.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.util.DebugLogger
import androidx.compose.ui.platform.LocalContext

@Composable
fun FullScreenImage(url: String) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .logger(DebugLogger())
        .build()

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = null,
        imageLoader = imageLoader,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentScale = ContentScale.Crop
    )
}
