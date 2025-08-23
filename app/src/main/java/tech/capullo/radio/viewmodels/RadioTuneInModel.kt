package tech.capullo.radio.viewmodels

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.capullo.radio.services.SnapclientService
import tech.capullo.radio.snapcast.SnapcastDiscoveryManager
import tech.capullo.radio.snapcast.SnapcastServer
import tech.capullo.radio.ui.AudioChannel
import tech.capullo.radio.ui.AudioSettings
import javax.inject.Inject

@HiltViewModel
class RadioTuneInModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val discoveryManager: SnapcastDiscoveryManager,
) : ViewModel() {

    private fun getSharedPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)

    fun saveLastServerText(text: String) {
        getSharedPreferences(applicationContext).edit {
            putString("my_text", text)
        }
    }

    fun getLastServerText(): String =
        getSharedPreferences(applicationContext).getString("my_text", "") ?: ""

    fun saveAudioSettings(settings: AudioSettings) {
        getSharedPreferences(applicationContext).edit {
            putInt("audio_channel", settings.audioChannel.ordinal)
            putInt("audio_latency", settings.latency)
            putFloat("audio_volume", settings.volume)
            putBoolean("persist_audio_settings", settings.persistSettings)
        }
    }

    fun getAudioSettings(): AudioSettings {
        val prefs = getSharedPreferences(applicationContext)
        val persistSettings = prefs.getBoolean("persist_audio_settings", false)

        return if (persistSettings) {
            AudioSettings(
                audioChannel = AudioChannel.entries.getOrElse(
                    prefs.getInt("audio_channel", AudioChannel.STEREO.ordinal),
                ) { AudioChannel.STEREO },
                latency = prefs.getInt("audio_latency", 0),
                volume = prefs.getFloat("audio_volume", 1.0f),
                persistSettings = persistSettings,
            )
        } else {
            AudioSettings() // Default settings
        }
    }

    // State to track if discovery is in progress
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    // Expose the discovered servers from the discovery manager
    val discoveredServers = discoveryManager.discoveredServers

    // Selected server
    private val _selectedServer = MutableStateFlow<SnapcastServer?>(null)
    val selectedServer: StateFlow<SnapcastServer?> = _selectedServer.asStateFlow()

    init {
        // Start discovery when the ViewModel is created
        startDiscovery()
    }

    fun startDiscovery() {
        viewModelScope.launch {
            _isDiscovering.value = true
            discoveryManager.startDiscovery()
        }
    }

    fun stopDiscovery() {
        discoveryManager.stopDiscovery()
        _isDiscovering.value = false
    }

    fun selectServer(server: SnapcastServer) {
        _selectedServer.value = server
        // Save the selected server IP for future use
        saveLastServerText(server.host)
    }

    fun startSnapclientService(server: SnapcastServer, audioChannel: AudioChannel) {
        val intent = Intent(applicationContext, SnapclientService::class.java).apply {
            putExtra(SnapclientService.KEY_IP, server.host)
            putExtra(SnapclientService.KEY_AUDIO_CHANNEL, audioChannel.ordinal)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
    }

    // For backward compatibility
    fun startSnapclientService(ip: String, audioChannel: AudioChannel) {
        val intent = Intent(applicationContext, SnapclientService::class.java).apply {
            putExtra(SnapclientService.KEY_IP, ip)
            putExtra(SnapclientService.KEY_AUDIO_CHANNEL, audioChannel.ordinal)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
    }

    fun restartSnapclientService(server: SnapcastServer, audioChannel: AudioChannel) {
        // Stop the current service
        val stopIntent = Intent(applicationContext, SnapclientService::class.java)
        applicationContext.stopService(stopIntent)

        // Start with new audio channel
        startSnapclientService(server, audioChannel)
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }
}
