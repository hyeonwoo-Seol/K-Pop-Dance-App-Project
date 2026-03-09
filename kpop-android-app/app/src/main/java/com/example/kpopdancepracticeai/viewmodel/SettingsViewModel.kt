package com.example.kpopdancepracticeai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kpopdancepracticeai.data.PracticeSettings
import com.example.kpopdancepracticeai.data.PracticeSettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = PracticeSettingsDataStore(application.applicationContext)

    val settings: StateFlow<PracticeSettings> = dataStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PracticeSettings()
    )

    fun setMirrorMode(enabled: Boolean) = viewModelScope.launch { dataStore.setMirrorMode(enabled) }
    fun setFrontCamera(enabled: Boolean) = viewModelScope.launch { dataStore.setFrontCamera(enabled) }
    fun setCountdownSeconds(seconds: Int) = viewModelScope.launch { dataStore.setCountdownSeconds(seconds) }
    fun setAutoUpload(enabled: Boolean) = viewModelScope.launch { dataStore.setAutoUpload(enabled) }
    fun setWifiOnlyUpload(enabled: Boolean) = viewModelScope.launch { dataStore.setWifiOnlyUpload(enabled) }
}
