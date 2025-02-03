package com.example.tvcontent.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: Int,
    val name: String
)

// Existing or updated content model
@Serializable
data class Content(
    val id: Int,
    val url: String,
    val is_video: Boolean
    // If you store videos, you might have video_url or something else
    // val video_url: String? = null
)

// A join model that represents an entry in `playlist_content`
@Serializable
data class PlaylistContent(
    val id: Int,
    val playlist_id: Int,
    val content_id: Int,
    val duration: Int
)

@Serializable
data class PlaylistItem(
    val content_id: Int,
    val is_video: Boolean,
    val url: String,
    val duration: Int
)

