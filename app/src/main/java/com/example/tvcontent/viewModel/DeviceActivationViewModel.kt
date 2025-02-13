package com.example.tvcontent.viewModel

import android.os.Debug
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tvcontent.util.saveDeviceLocally
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID

class DeviceActivationViewModel(
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    var deviceId: UUID = UUID.randomUUID()
    var activationCode by mutableStateOf<String?>(null)
    var userId by mutableStateOf<String?>(null)
    var isActivated by mutableStateOf(false)

    /**
     * Called when user presses the "Generate Pairing Code" button.
     */
    fun generatePairingCode() {
        viewModelScope.launch {
            val code = createActivationCode(deviceId)
            activationCode = code
            startPolling(code)
        }
    }

    private suspend fun createActivationCode(deviceId: UUID): String? {
        val response = supabaseClient.postgrest
            .rpc("create_activation_code", mapOf("_device_id" to deviceId.toString()))

        return response.decodeAs() // raw string
    }

    private fun startPolling(code: String?) {
        if (code == null) return
        viewModelScope.launch {
            while (!isActivated) {
                val claimedUserId = checkActivation(code)
                if (claimedUserId != null) {
                    // Device is now activated
                    userId = claimedUserId
                    isActivated = true
                } else {
                    delay(5000)
                }
            }
        }
    }

    @Serializable
    data class ActivationRow(val user_id: String?)

    private suspend fun checkActivation(code: String): String? {
        val row = supabaseClient.postgrest["activation_codes"]
            .select(Columns.list("user_id")) {
                filter { eq("code", code) }
                limit(1)
                single()
            }
            .decodeAs<ActivationRow>()

        return row.user_id
    }

    companion object {
        fun Factory(supabaseClient: SupabaseClient) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(DeviceActivationViewModel::class.java)) {
                    return DeviceActivationViewModel(supabaseClient) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
