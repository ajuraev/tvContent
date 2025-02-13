package com.example.tvcontent.util

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.content.Context
import android.content.SharedPreferences

object SecurePreferences {
    private const val FILE_NAME = "secure_device_prefs"

    fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}

fun saveDeviceLocally(context: Context, deviceId: String, deviceName: String?) {
    val prefs = SecurePreferences.getEncryptedPrefs(context)
    prefs.edit().apply {
        putString("device_id", deviceId)
        putString("device_name", deviceName)
        apply()
    }
}


fun loadDeviceLocally(context: Context): Pair<String?, String?> {
    val prefs = SecurePreferences.getEncryptedPrefs(context)
    val deviceId = prefs.getString("device_id", null)
    val deviceName = prefs.getString("device_name", null)
    return deviceId to deviceName
}
