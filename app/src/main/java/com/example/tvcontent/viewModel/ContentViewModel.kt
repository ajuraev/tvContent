package com.example.tvcontent.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tvcontent.data.model.Playlist
import com.example.tvcontent.data.model.PlaylistItem
import com.example.tvcontent.util.SecurePreferences
import com.example.tvcontent.util.loadDeviceLocally
import com.example.tvcontent.util.saveDeviceLocally
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class DevicePlaylistParams(
    val device_id_param: String
)

@Serializable
data class DeviceHeartbeat(
    val deviceId: String,
    val heartbeated_at: String
)

@Serializable
data class DeviceInfo(
    val id: String,
    val name: String? // Device name can be nullable
)

class ContentViewModel(
    private val supabaseClient: SupabaseClient,
    private val applicationContext: Context
) : ViewModel() {

    // region Device ID and Playlist Items
    private val _deviceIdFlow = MutableStateFlow<String?>(null)
    val deviceIdFlow = _deviceIdFlow.asStateFlow()

    // Add deviceNameFlow
    private val _deviceNameFlow = MutableStateFlow<String?>(null)
    val deviceNameFlow = _deviceNameFlow.asStateFlow()


    private val _currentPlaylistItemsFlow = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val currentPlaylistItemsFlow = _currentPlaylistItemsFlow.asStateFlow()
    // endregion

    init {
        // 1) Subscribe to realtime for content changes
        viewModelScope.launch {
            subscribeToPlaylistAndContentChanges()
        }
        // 2) Attempt to load items (in case device was set previously)
        viewModelScope.launch {
            loadPlaylistItems()
        }
        // 3) Start the heartbeat loop
        startHeartbeat()
        // 4) (Optional) Start periodic device existence check
        startPeriodicDeviceCheck()
    }

    // region Device Existence Verification
    private suspend fun verifyDeviceExists(deviceId: String): Pair<Boolean, String?> {
        return try {
            val result = supabaseClient.postgrest["device_heartbeats_view"] // Query the 'devices' table
                .select(Columns.list("id", "name")) { // Select both 'id' and 'name'
                    filter { eq("id", deviceId) }
                    limit(1)
                }
                .decodeList<DeviceInfo>() // Decode to a list of DeviceInfo

            if (result.isNotEmpty()) {
                val deviceInfo = result.first() // Get the first (and only) element
                Log.d("ContentViewModel", "Verified device ${deviceInfo.id} exists. Name: ${deviceInfo.name}")
                Pair(true, deviceInfo.name) // Return true and the device name
            } else {
                Log.d("ContentViewModel", "Device $deviceId not found.")
                Pair(false, null) // Return false and null name
            }
        } catch (e: Exception) {
            Log.e("ContentViewModel", "verifyDeviceExists: Error: ${e.localizedMessage}", e)
            Pair(false, null) // Return false and null name on error
        }
    }

// endregion

    // region Load / Save / Clear Device

    fun loadDevice(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val (loadedId, loadedName) = loadDeviceLocally(context)
            _deviceIdFlow.value = loadedId
            _deviceNameFlow.value = loadedName // Also set the device name

            // Check if device exists and get its name
            if (!loadedId.isNullOrEmpty()) {
                val (exists, deviceName) = verifyDeviceExists(loadedId)
                if (exists) {
                    //Update the flow with the latest name from the database
                    _deviceNameFlow.value = deviceName
                } else {
                    clearDevice(context) // remove from local
                }
            }

            // Switch to main to load items
            launch(Dispatchers.Main) {
                loadPlaylistItems()
            }
        }
    }

    /**
     * Save the device ID to secure prefs, update the flow,
     * then load items for that device.
     */
    fun saveDevice(context: Context, deviceId: String, deviceName: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            saveDeviceLocally(context, deviceId, deviceName)
            _deviceIdFlow.value = deviceId
            _deviceNameFlow.value = deviceName // Also update the device name flow

            launch(Dispatchers.Main) {
                loadPlaylistItems()
            }
        }
    }

    /**
     * Clears device ID from local storage and resets the flow to null.
     */
    fun onLogout(context: Context) {
        viewModelScope.launch {
            clearDevice(context)
        }
    }

    private fun clearDevice(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = SecurePreferences.getEncryptedPrefs(context)
            prefs.edit()
                .remove("device_id")
                .remove("device_name")
                .apply()

            _deviceIdFlow.value = null
            _deviceNameFlow.value = null // Also clear the device name
        }
    }
    // endregion

    // region Heartbeat
    /**
     * Heartbeat loop: runs indefinitely, every 10 seconds, tries to upsert heartbeated_at.
     */
    private fun startHeartbeat() {
        viewModelScope.launch {
            while (isActive) {
                val currentDeviceId = _deviceIdFlow.value
                if (!currentDeviceId.isNullOrEmpty()) {
                    Log.d("ContentViewModel", "Sending heartbeat for deviceId=$currentDeviceId")
                    sendHeartbeat(currentDeviceId)
                } else {
                    Log.d("ContentViewModel", "No deviceId yet, skipping heartbeat")
                }
                delay(60_000) // 10s
            }
        }
    }

    private suspend fun sendHeartbeat(deviceId: String) {
        try {
            supabaseClient.postgrest["device_heartbeats"]
                .upsert(
                    mapOf(
                        "device_id" to deviceId,
                        "heartbeated_at" to "now()"
                    ),
                    onConflict = "device_id"
                ) {
                    // By default, upsert merges (updates) if conflict
                    // so this is enough for a heartbeat
                }
            Log.d("ContentViewModel", "Heartbeat sent for $deviceId")
        } catch (e: HttpRequestException) {
            Log.e("ContentViewModel", "HTTP error in heartbeat: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e("ContentViewModel", "Error sending heartbeat: ${e.localizedMessage}", e)
        }
    }
    // endregion

    // region (Optional) Periodic Device Existence Check
    /**
     * If you'd like to re-verify the device row each hour or so, uncomment in init.
     */
    private fun startPeriodicDeviceCheck() {
        viewModelScope.launch {
            while (isActive) {
                val currentId = _deviceIdFlow.value
                if (!currentId.isNullOrEmpty()) {
                    val (stillExists, deviceName) = verifyDeviceExists(currentId)
                    if (stillExists) {
                        _deviceNameFlow.value = deviceName
                    }
                    else{
                        Log.w("ContentViewModel", "Device $currentId no longer exists on server, clearing local.")
                        clearDevice(applicationContext)
                        // break or keep looping, up to you
                        //break
                    }
                }
                delay(60_000) // 10 mins
            }
        }
    }
    // endregion

    // region Playlist Items
    /**
     * If there's a device set, load its latest playlist items.
     */
    private fun loadPlaylistItems() {
        viewModelScope.launch {
            val deviceId = _deviceIdFlow.value
            if (!deviceId.isNullOrEmpty()) {
                val items = fetchLatestPlaylistItemsForDevice(deviceId)
                _currentPlaylistItemsFlow.value = items
                Log.d("ContentViewModel", "Loaded ${items.size} items for device=$deviceId")
            } else {
                Log.e("ContentViewModel", "No device set; cannot load playlist items.")
                _currentPlaylistItemsFlow.value = emptyList()
            }
        }
    }

    private suspend fun fetchLatestPlaylistItemsForDevice(deviceId: String): List<PlaylistItem> {
        return try {
            val rpcParams = DevicePlaylistParams(device_id_param = deviceId)
            val playlists = supabaseClient.postgrest
                .rpc("get_device_playlists", rpcParams)
                .decodeList<Playlist>()

            Log.d("ContentViewModel", "Fetched playlists: $playlists")

            val latestPlaylistId = playlists.firstOrNull()?.id ?: return emptyList()

            supabaseClient.postgrest["device_playlist_content_view"]
                .select(Columns.ALL) {
                    filter { eq("playlist_id", latestPlaylistId) }
                    order("order", Order.ASCENDING)
                }
                .decodeList()
        } catch (e: Exception) {
            Log.e("ContentViewModel", "Error fetching playlist: ${e.localizedMessage}", e)
            emptyList()
        }
    }
    // endregion

    // region Realtime Subscription
    /**
     * Subscribes to changes in 'playlist_content' table.
     * If there's an insert/update/delete, reload items.
     */

    fun resubscribeToRealtime() {
        viewModelScope.launch {
            subscribeToPlaylistAndContentChanges()
        }
    }
    private suspend fun subscribeToPlaylistAndContentChanges() {
        // Disconnect first to avoid multiple subscriptions
        supabaseClient.realtime.disconnect()
        supabaseClient.realtime.connect()

        val contentChannel = supabaseClient.channel("playlistContentChannel")

        val insertFlow = contentChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "playlist_content"
        }
        val updateFlow = contentChannel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "playlist_content"
        }
        val deleteFlow = contentChannel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
            table = "playlist_content"
        }

        contentChannel.subscribe()

        // Listen for changes, reload items
        viewModelScope.launch {
            insertFlow.collect {
                Log.d("Realtime", "INSERT in playlist_content: $it")
                loadPlaylistItems()
            }
        }
        viewModelScope.launch {
            updateFlow.collect {
                Log.d("Realtime", "UPDATE in playlist_content: $it")
                loadPlaylistItems()
            }
        }
        viewModelScope.launch {
            deleteFlow.collect {
                Log.d("Realtime", "DELETE in playlist_content: $it")
                loadPlaylistItems()
            }
        }
    }
    // endregion

    // region Factory
    class Factory(private val supabaseClient: SupabaseClient, private val applicationContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ContentViewModel::class.java)) {
                return ContentViewModel(supabaseClient, applicationContext) as T // Pass context to ViewModel
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
    // endregion
}