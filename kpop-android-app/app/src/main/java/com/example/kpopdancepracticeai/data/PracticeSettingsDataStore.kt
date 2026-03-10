package com.example.kpopdancepracticeai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.practiceSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "practice_settings")

data class PracticeSettings(
    val isMirrorMode: Boolean = false,
    val isFrontCamera: Boolean = true,
    val countdownSeconds: Int = 3,
    val isAutoUpload: Boolean = true,
    val isWifiOnlyUpload: Boolean = true,
    val isServerUploadEnabled: Boolean = false
)

class PracticeSettingsDataStore(private val context: Context) {

    private object Keys {
        val MIRROR_MODE = booleanPreferencesKey("mirror_mode")
        val FRONT_CAMERA = booleanPreferencesKey("front_camera")
        val COUNTDOWN_SECONDS = intPreferencesKey("countdown_seconds")
        val AUTO_UPLOAD = booleanPreferencesKey("auto_upload")
        val WIFI_ONLY_UPLOAD = booleanPreferencesKey("wifi_only_upload")
        val SERVER_UPLOAD_ENABLED = booleanPreferencesKey("server_upload_enabled")
    }

    val settingsFlow: Flow<PracticeSettings> = context.practiceSettingsDataStore.data.map { pref ->
        PracticeSettings(
            isMirrorMode = pref[Keys.MIRROR_MODE] ?: false,
            isFrontCamera = pref[Keys.FRONT_CAMERA] ?: true,
            countdownSeconds = pref[Keys.COUNTDOWN_SECONDS] ?: 3,
            isAutoUpload = pref[Keys.AUTO_UPLOAD] ?: true,
            isWifiOnlyUpload = pref[Keys.WIFI_ONLY_UPLOAD] ?: true,
            isServerUploadEnabled = pref[Keys.SERVER_UPLOAD_ENABLED] ?: false
        )
    }

    suspend fun setMirrorMode(enabled: Boolean) {
        context.practiceSettingsDataStore.edit { it[Keys.MIRROR_MODE] = enabled }
    }

    suspend fun setFrontCamera(enabled: Boolean) {
        context.practiceSettingsDataStore.edit { it[Keys.FRONT_CAMERA] = enabled }
    }

    suspend fun setCountdownSeconds(seconds: Int) {
        context.practiceSettingsDataStore.edit { it[Keys.COUNTDOWN_SECONDS] = seconds }
    }

    suspend fun setAutoUpload(enabled: Boolean) {
        context.practiceSettingsDataStore.edit { it[Keys.AUTO_UPLOAD] = enabled }
    }

    suspend fun setWifiOnlyUpload(enabled: Boolean) {
        context.practiceSettingsDataStore.edit { it[Keys.WIFI_ONLY_UPLOAD] = enabled }
    }

    suspend fun setServerUploadEnabled(enabled: Boolean) {
        context.practiceSettingsDataStore.edit { it[Keys.SERVER_UPLOAD_ENABLED] = enabled }
    }
}
