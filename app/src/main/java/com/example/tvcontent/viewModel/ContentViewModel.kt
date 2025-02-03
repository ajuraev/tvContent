package com.example.tvcontent.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tvcontent.data.model.Content
import com.example.tvcontent.data.model.Playlist
import com.example.tvcontent.data.model.PlaylistContent
import com.example.tvcontent.data.model.PlaylistItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class StorePlaylistParams(
    val store_id_param: Int
)

@Serializable
data class Store(
    val id: Int,
    val name: String,
    // Add other fields as needed
)

class ContentViewModel(private val supabaseClient: SupabaseClient) : ViewModel() {

    private val _currentPlaylistItemsFlow = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val currentPlaylistItemsFlow = _currentPlaylistItemsFlow.asStateFlow()

    /**
     * Holds the currently selected store, loaded from SharedPreferences.
     * Null = no store selected yet.
     */
    private val _selectedStoreFlow = MutableStateFlow<String?>(null)
    val selectedStoreFlow = _selectedStoreFlow.asStateFlow()

    private val _storesFlow = MutableStateFlow<List<Store>>(emptyList())
    val storesFlow = _storesFlow.asStateFlow()


    init {
        // Subscribe to Realtime
        viewModelScope.launch {
            subscribeToPlaylistAndContentChanges()
        }
        // Optionally fetch existing content
        viewModelScope.launch {
            loadPlaylistItems()
        }
    }

    fun resubscribeToRealtime() {
        viewModelScope.launch {
            subscribeToPlaylistAndContentChanges()
        }
    }

    private fun loadPlaylistItems() {
        viewModelScope.launch {
            val storeId = selectedStoreFlow.value
            if (storeId != null) {
                val items = fetchLatestPlaylistItemsForStore(storeId)
                Log.d("Playlist", items.toString() + storeId, )
                _currentPlaylistItemsFlow.value = items
            } else {
                Log.e("ContentViewModel", "No store selected")
                _currentPlaylistItemsFlow.value = emptyList()
            }
        }
    }

    fun fetchStores() {
        viewModelScope.launch {
            try {
                val stores = supabaseClient.postgrest["stores"]
                    .select(Columns.ALL) {
                        order("name", Order.ASCENDING)
                    }
                    .decodeList<Store>()
                _storesFlow.value = stores
            } catch (e: Exception) {
                Log.e("ContentViewModel", "Error fetching stores: ${e.localizedMessage}", e)
                _storesFlow.value = emptyList()
            }
        }
    }

    private suspend fun fetchLatestPlaylistItemsForStore(storeId: String?): List<PlaylistItem> {
        return try {
            if (storeId == null) {
                Log.e("ContentViewModel", "Store ID is null")
                return emptyList()
            }

            val rpcParams = StorePlaylistParams(store_id_param = storeId.toInt())
            val playlists = supabaseClient.postgrest.rpc("get_store_playlists", rpcParams)
                .decodeList<Playlist>()

            val latestPlaylistId = playlists.firstOrNull()?.id

            if (latestPlaylistId != null) {
                // Step 2: Fetch items for the latest playlist using our view
                supabaseClient.postgrest["store_playlist_content_view"]
                    .select(Columns.ALL) {
                        filter {
                            eq("playlist_id", latestPlaylistId)
                        }
                        order("order", Order.ASCENDING) // Add this if you want to maintain display order
                    }
                    .decodeList<PlaylistItem>()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ContentViewModel", "Error fetching latest playlist items: ${e.localizedMessage}", e)
            emptyList()
        }
    }

    private suspend fun subscribeToPlaylistAndContentChanges() {
        supabaseClient.realtime.disconnect()
        supabaseClient.realtime.connect()

        // Playlist Content Channel: Listen to INSERT, UPDATE, DELETE
        val contentChannel = supabaseClient.channel("playlistContentChannel")
        val contentInsertFlow = contentChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "playlist_content"
        }
        val contentUpdateFlow = contentChannel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "playlist_content"
        }
        val contentDeleteFlow = contentChannel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
            table = "playlist_content"
        }

        contentChannel.subscribe()

        // Collect events from playlist_content table
        viewModelScope.launch {
            contentInsertFlow.collect { change ->
                Log.d("Realtime", "INSERT in playlist_content: $change")
                loadPlaylistItems() // Reload items or handle changes as necessary
            }
        }
        viewModelScope.launch {
            contentUpdateFlow.collect { change ->
                Log.d("Realtime", "UPDATE in playlist_content: $change")
                loadPlaylistItems() // Reload items or handle changes as necessary
            }
        }
        viewModelScope.launch {
            contentDeleteFlow.collect { change ->
                Log.d("Realtime", "DELETE in playlist_content: $change")
                loadPlaylistItems() // Reload items or handle changes as necessary
            }
        }

        // Playlist Table Channel: Listen to UPDATES
//        val playlistChannel = supabaseClient.channel("playlistChannel")
//        val playlistUpdateFlow = playlistChannel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
//            table = "playlists"
//        }

        //playlistChannel.subscribe()

        // Collect events from playlist table
//        viewModelScope.launch {
//            playlistUpdateFlow.collect { change ->
//                Log.d("Realtime", "UPDATE in playlists: $change")
//                loadPlaylistItems() // Reload items or handle changes as necessary
//            }
//        }
    }


    // region Local Storage (SharedPreferences)
    /**
     * Load the stored store name from SharedPreferences and set _selectedStoreFlow.
     */
    fun loadSelectedStore(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("store_prefs", Context.MODE_PRIVATE)
            val storedValue = prefs.getString("selected_store", null)
            // Switch to Main thread if you want
            _selectedStoreFlow.value = storedValue

            loadPlaylistItems()
        }
    }

    /**
     * Save the store name to SharedPreferences and update _selectedStoreFlow.
     */
    fun saveSelectedStore(context: Context, newStore: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("store_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("selected_store", newStore.toString()).apply()
            _selectedStoreFlow.value = newStore.toString()
            loadPlaylistItems()
        }
    }
    // endregion

    // region Factory
    class Factory(private val supabaseClient: SupabaseClient) : ViewModelProvider.Factory {
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
