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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.capullo.radio.services.SnapclientService
import tech.capullo.radio.snapcast.ClientOnConnect
import tech.capullo.radio.snapcast.ClientOnDisconnect
import tech.capullo.radio.snapcast.ClientOnLatencyChanged
import tech.capullo.radio.snapcast.ClientOnVolumeChanged
import tech.capullo.radio.snapcast.Group
import tech.capullo.radio.snapcast.ServerGetStatusResponse
import tech.capullo.radio.snapcast.ServerOnUpdate
import tech.capullo.radio.snapcast.SnapcastControlClient
import tech.capullo.radio.snapcast.SnapcastJSONRPCResponse
import tech.capullo.radio.snapcast.SnapclientProcess
import tech.capullo.radio.snapcast.Stream
import tech.capullo.radio.snapcast.StreamMetadata
import tech.capullo.radio.snapcast.StreamOnProperties
import tech.capullo.radio.ui.model.AudioChannel

data class NowPlayingUiState(
    val audioChannelState: AudioChannel,
    val serverIp: String,
    val snapclientProcessConnectionState: SnapclientProcess.ConnectionState,
    val snapcastControlClientConnectionState: SnapcastControlClient.ConnectionState,
    val playbackStatus: String? = null,
    val metadata: StreamMetadata? = null,
)

@HiltViewModel(assistedFactory = NowPlayingViewModel.Factory::class)
class NowPlayingViewModel @AssistedInject constructor(
    @ApplicationContext private val applicationContext: Context,
    @Assisted val serverIp: String,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(serverIp: String): NowPlayingViewModel
    }

    var snapcastControlClient = SnapcastControlClient(
        serverIp,
    )

    // updated by notifications flow from snapcastControlClient
    private var _groups = mutableStateListOf<Group>()
    val groups: List<Group> = _groups

    private var _streams = mutableStateListOf<Stream>()
    val streams: List<Stream> = _streams

    private val audioChannelState = MutableStateFlow(AudioChannel.STEREO)

    private val snapclientProcessConnectionState =
        MutableStateFlow(SnapclientProcess.ConnectionState.STARTING)

    private val playbackStatusState = MutableStateFlow<String?>(null)
    private val metadataState = MutableStateFlow<StreamMetadata?>(null)

    val nowPlayingUiState: StateFlow<NowPlayingUiState> = combine(
        audioChannelState,
        snapcastControlClient.connectionState,
        snapclientProcessConnectionState,
        playbackStatusState,
        metadataState,
    ) {
            audioChannelState,
            snapcastControlClientConnectionState,
            snapclientProcessConnectionState,
            playbackStatus,
            metadata,
        ->
        NowPlayingUiState(
            audioChannelState = audioChannelState,
            serverIp = serverIp,
            snapclientProcessConnectionState = snapclientProcessConnectionState,
            snapcastControlClientConnectionState = snapcastControlClientConnectionState,
            playbackStatus = playbackStatus,
            metadata = metadata,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = NowPlayingUiState(
            audioChannelState = AudioChannel.STEREO,
            serverIp = serverIp,
            snapclientProcessConnectionState = SnapclientProcess.ConnectionState.STARTING,
            snapcastControlClientConnectionState = SnapcastControlClient.ConnectionState.STARTING,
        ),
    )

    private var binder: SnapclientService.SnapclientBinder? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as SnapclientService.SnapclientBinder
            isBound = true

            // collect audio channel settings (they can be changed from the UI)
            viewModelScope.launch {
                launch {
                    binder?.getConnectionStateFlow()?.collect { newConnectionState ->
                        snapclientProcessConnectionState.update { newConnectionState }
                    }
                }
                launch {
                    binder?.getAudioChannelFlow()?.collect { audioChannel ->
                        audioChannelState.update { audioChannel }
                    }
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
            snapcastControlClient.initialize() // <--- will suspend until connected

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
                _streams.clear()
                _streams.addAll(notification.result.server.streams)
                updateStreamInfo()
            }

            is ServerOnUpdate -> {
                _groups.clear()
                _groups.addAll(notification.params.server.groups)
                _streams.clear()
                _streams.addAll(notification.params.server.streams)
                updateStreamInfo()
            }

            is StreamOnProperties -> {
                val index = _streams.indexOfFirst { it.id == notification.params.id }
                if (index != -1) {
                    _streams[index] =
                        _streams[index].copy(properties = notification.params.properties)
                }
                updateStreamInfo()
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

            is ClientOnLatencyChanged -> {
                val targetGroupIndex = _groups.find { group ->
                    group.clients.any { client -> client.id == notification.params.clientId }
                }?.let { group ->
                    _groups.indexOf(group)
                }

                targetGroupIndex?.let { i ->
                    val updatedClientList = _groups[i].clients.map { client ->
                        if (client.id == notification.params.clientId) {
                            client.copy(
                                config = client.config.copy(latency = notification.params.latency),
                            )
                        } else {
                            client
                        }
                    }
                    _groups[i] = _groups[i].copy(clients = updatedClientList)
                }
            }

            is ClientOnDisconnect -> {
                // index of the group we are going to replace
                val targetGroupIndex = _groups.find { group ->
                    group.clients.any { client -> client.id == notification.params.client.id }
                }?.let { group ->
                    _groups.indexOf(group)
                }

                targetGroupIndex?.let { i ->
                    val updatedClientList = _groups[i].clients.map { client ->
                        if (client.id == notification.params.client.id) {
                            client.copy(
                                connected = notification.params.client.connected,
                            )
                        } else {
                            client
                        }
                    }
                    _groups[i] = _groups[i].copy(clients = updatedClientList)
                }
            }

            is ClientOnConnect -> {
                // index of the group we are going to replace
                val targetGroupIndex = _groups.find { group ->
                    group.clients.any { client -> client.id == notification.params.client.id }
                }?.let { group ->
                    _groups.indexOf(group)
                }

                targetGroupIndex?.let { i ->
                    val updatedClientList = _groups[i].clients.map { client ->
                        if (client.id == notification.params.client.id) {
                            client.copy(
                                connected = notification.params.client.connected,
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

    private fun updateStreamInfo() {
        // For now, we use the first stream's info
        _streams.firstOrNull()?.let { stream ->
            playbackStatusState.value = stream.properties.playbackStatus
            metadataState.value = stream.properties.metadata
        }
    }

    fun onStreamControl(command: String) {
        viewModelScope.launch {
            _streams.firstOrNull()?.let { stream ->
                snapcastControlClient.sendStreamControl(stream.id, command)
            }
        }
    }

    fun onClientVolumeChange(clientId: String, muted: Boolean, volume: Int) {
        viewModelScope.launch {
            snapcastControlClient.sendSetVolume(clientId, muted, volume)
        }
    }

    fun onClientLatencyChange(clientId: String, latency: Int) {
        viewModelScope.launch {
            snapcastControlClient.sendSetLatency(clientId, latency)
        }
    }

    companion object {
        private val TAG = NowPlayingViewModel::class.simpleName
    }

    object PreferencesKeys {
        val LAST_SERVER_TEXT = stringPreferencesKey("last_server_text")
    }
}
