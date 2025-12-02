package tech.capullo.radio.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.capullo.radio.services.SnapclientService
import tech.capullo.radio.snapcast.ClientOnVolumeChanged
import tech.capullo.radio.snapcast.Group
import tech.capullo.radio.snapcast.ServerGetStatusResponse
import tech.capullo.radio.snapcast.ServerOnUpdate
import tech.capullo.radio.snapcast.SnapcastControlClient
import tech.capullo.radio.snapcast.SnapcastJSONRPCResponse
import tech.capullo.radio.ui.model.AudioChannel

data class NowPlayingUiState(val audioChannel: AudioChannel, val serverIp: String)

@HiltViewModel(assistedFactory = NowPlayingViewModel.Factory::class)
class NowPlayingViewModel @AssistedInject constructor(
    @ApplicationContext private val applicationContext: Context,
    @Assisted val serverIp: String,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(serverIp: String): NowPlayingViewModel
    }

    private var _groups = mutableStateListOf<Group>()
    val groups: List<Group> = _groups

    private var _nowPlayingUiState = MutableStateFlow(
        NowPlayingUiState(
            audioChannel = AudioChannel.STEREO,
            serverIp = serverIp,
        ),
    )
    val nowPlayingUiState: StateFlow<NowPlayingUiState> = _nowPlayingUiState

    var snapcastControlClient = SnapcastControlClient(
        serverIp,
    )

    private var binder: SnapclientService.SnapclientBinder? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as SnapclientService.SnapclientBinder
            isBound = true

            // collect audio channel settings (they can be changed from the UI)
            viewModelScope.launch {
                binder?.getAudioChannelFlow()?.collect { audioChannel ->
                    _nowPlayingUiState.update { it.copy(audioChannel = audioChannel) }
                }
            }

            startSnapcastControlClient()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
            isBound = false
        }
    }

    init {
        Log.d(TAG, "NowPlayingViewModel got created with: $serverIp")

        val intent = Intent(applicationContext, SnapclientService::class.java).apply {
            putExtra(SnapclientService.KEY_IP, serverIp)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }

        applicationContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Save the server host address
        viewModelScope.launch {
            applicationContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.LAST_SERVER_TEXT] = serverIp
            }
        }
    }

    fun startSnapcastControlClient() {
        viewModelScope.launch {
            // TODO: loading/success/error uiStates
            // goes together with session connection retry in control client
            snapcastControlClient.initialize()
            snapcastControlClient.sendGetStatus()
            snapcastControlClient.notifications.collect { notification ->
                notification?.let { handleNotification(it) }
            }
        }
    }

    fun updateAudioChannel(channel: AudioChannel) {
        binder?.updateAudioChannel(channel)
    }

    fun handleNotification(notification: SnapcastJSONRPCResponse) {
        Log.d(TAG, "Handling notification: $notification")

        when (notification) {
            is ServerGetStatusResponse -> {
                _groups.clear()
                _groups.addAll(notification.result.server.groups)
            }

            is ServerOnUpdate -> {
                _groups.clear()
                _groups.addAll(notification.params.server.groups)
            }

            is ClientOnVolumeChanged -> {
                // index of the group we are going to replace
                val targetGroupIndex = _groups.find { group ->
                    group.clients.any { client -> client.id == notification.params.clientId }
                }?.let { group ->
                    _groups.indexOf(group)
                }

                targetGroupIndex?.let { i ->
                    val updatedClientList = _groups[i].clients.map { client ->
                        if (client.id == notification.params.clientId) {
                            client.copy(
                                config = client.config.copy(volume = notification.params.volume),
                            )
                        } else {
                            client
                        }
                    }
                    _groups[i] = _groups[i].copy(clients = updatedClientList)
                }
            }
        }
    }

    fun onClientVolumeChange(clientId: String, muted: Boolean, volume: Int) {
        viewModelScope.launch {
            snapcastControlClient.sendSetVolume(clientId, muted, volume)
        }
    }

    companion object {
        private val TAG = NowPlayingViewModel::class.simpleName
    }

    object PreferencesKeys {
        val LAST_SERVER_TEXT = stringPreferencesKey("last_server_text")
    }
}
