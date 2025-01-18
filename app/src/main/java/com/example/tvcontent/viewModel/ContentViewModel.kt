package com.example.tvcontent.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tvcontent.data.model.Content
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * A ViewModel responsible for:
 * - Subscribing to the 'content' table in Realtime
 * - Holding a list of newly inserted rows
 *   Each row is represented as a Pair(imageUrl, duration) or
 *   possibly a data class if you prefer.
 */
class ContentViewModel(private val supabaseClient: SupabaseClient) : ViewModel() {

    // Use a flow to collect newly inserted rows
    private val _contentInserts = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val contentInserts = _contentInserts.asStateFlow()

    init {
        // Instead of calling the suspend function directly...
//         call it inside a coroutine.
        viewModelScope.launch {
            subscribeToContentInserts()
        }
    }

    private suspend fun subscribeToContentInserts() {
        supabaseClient.realtime.connect()
        val channel = supabaseClient.channel("contentChannel")
        val insertFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "content"
        }

        channel.subscribe()

        // Collect the flow directly here
        insertFlow.collect { change ->
            val recordMap = change.record // Map<String, JsonElement>
            try {
                // Convert Map<String, JsonElement> to JsonObject
                val recordJson = JsonObject(recordMap)

                // Decode using Kotlin serialization
                val contentItem = Json.decodeFromJsonElement<Content>(recordJson)
                Log.d("Collector", "Decoded contentItem = $contentItem")

                // Now you can access fields directly:
                val imageUrl = contentItem.image_url
                val duration = contentItem.duration

                _contentInserts.update { oldList ->
                    oldList + (imageUrl to duration)
                }
                Log.d("Collector", "Content after: ${_contentInserts.value}")

            } catch (e: Exception) {
                Log.e("Collector", "Exception in flow collector", e)
            }
        }
    }
/**
 * OPTIONAL: If you want to manually query existing rows from the DB
 * (e.g. using Postgrest) before receiving Realtime inserts, you can
 * do that here. Or you can reuse your old OkHttp approach, etc.
 */
suspend fun fetchExistingContent() {
    try {
        // Fetch content as a list of `Content` objects
        val result = supabaseClient.postgrest["content"]
            .select(Columns.ALL)
            .decodeList<Content>() // Use the Content data class here

        // Append the fetched data to the existing state
        _contentInserts.update { oldList ->
            oldList + result.map { it.image_url to it.duration }
        }
    } catch (e: Exception) {
        // Log any errors for debugging
        Log.e("ContentViewModel", "Error fetching existing content: ${e.localizedMessage}", e)
    }
}


fun onFetchExistingContentClicked() {
    viewModelScope.launch {
        fetchExistingContent()
    }
}

// region Boilerplate for creating the ViewModel with a factory
class Factory(
    private val supabaseClient: SupabaseClient
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContentViewModel::class.java)) {
            return ContentViewModel(supabaseClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
// endregion
}
