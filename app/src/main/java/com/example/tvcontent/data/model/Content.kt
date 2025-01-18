package com.example.tvcontent.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Content(
    val id: Int,               // Include the id field
    val created_at: String,    // Include the created_at field
    val image_url: String,
    val duration: Int
)
