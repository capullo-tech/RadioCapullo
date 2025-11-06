package tech.capullo.radio.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.data.RadioRepository.IPv4AddressesResult
import tech.capullo.radio.services.RadioBroadcasterService
import tech.capullo.radio.snapcast.Client
import tech.capullo.radio.snapcast.ClientOnVolumeChanged
import tech.capullo.radio.snapcast.Group
import tech.capullo.radio.snapcast.ServerGetStatusResponse
import tech.capullo.radio.snapcast.ServerOnUpdate
import tech.capullo.radio.snapcast.SnapcastControlClient
import tech.capullo.radio.snapcast.SnapcastJSONRPCResponse
import tech.capullo.radio.ui.model.AudioChannel
import javax.inject.Inject

data class BroadcasterUiState(
    val ipv4AddressesResult: IPv4AddressesResult,
    val audioChannel: AudioChannel,
)

data class GroupUIState(val displayName: String, val isMuted: Boolean, val clients: List<Client>)

@HiltViewModel
class BroadcasterViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val radioRepository: RadioRepository,
) : ViewModel() {

    // Since we are being the broadcaster in this case, the snapclient is connected to our own
    // running snapserver, therefore we hardcode the "localhost" connection address
    private val snapcastControlClient = SnapcastControlClient(
        "127.0.0.1",
    )

    private var _snapserverGroups = mutableStateListOf<Client>()
    val snapserverGroups: List<Client> = _snapserverGroups

    private val _uiState = MutableStateFlow(
        BroadcasterUiState(
            ipv4AddressesResult = IPv4AddressesResult.Loading,
            audioChannel = AudioChannel.STEREO,
        ),
    )
    val uiState: StateFlow<BroadcasterUiState> = _uiState.asStateFlow()

    private var mBound: Boolean = false
    private var mService: RadioBroadcasterService.LocalBinder? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RadioBroadcasterService.LocalBinder
            mBound = true
            mService = binder

            startSnapcastControlClient()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
            mService = null
        }
    }

    private var initializeCalled = false

    fun initialize() {
        if (initializeCalled) return
        initializeCalled = true

        viewModelScope.launch { refreshIPv4Addresses() }

        startBroadcasterService()
    }

    fun startBroadcasterService() {
        val intent = Intent(appContext, RadioBroadcasterService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }

        appContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun updateAudioChannel(audioChannel: AudioChannel) {
        _uiState.update { it.copy(audioChannel = audioChannel) }
        mService?.updateAudioChannel(audioChannel)
    }

    fun refreshIPv4Addresses() {
        _uiState.update { it.copy(ipv4AddressesResult = IPv4AddressesResult.Loading) }
        viewModelScope.launch {
            val ipv4AddressesResult = radioRepository.getIPv4Addresses()
            _uiState.update { it.copy(ipv4AddressesResult = ipv4AddressesResult) }
        }
    }

    fun startSnapcastControlClient() {
        viewModelScope.launch {
            // TODO: loading/success/error uiStates
            // goes together with session connection retry in control client
            delay(3_000)
            snapcastControlClient.initialize()
            snapcastControlClient.sendGetStatus()
            snapcastControlClient.notifications.collect { notification ->
                notification?.let { handleNotification(it) }
            }
        }
    }

    fun handleNotification(notification: SnapcastJSONRPCResponse) {
        Log.d(TAG, "Handling notification: $notification")

        when (notification) {
            is ServerGetStatusResponse -> {
                _snapserverGroups.clear()
                _snapserverGroups.addAll(
                    notification.result.server.groups.flatMap { group -> group.clients },
                )
            }

            is ServerOnUpdate -> {
                _snapserverGroups.clear()
                _snapserverGroups.addAll(
                    notification.params.server.groups.flatMap { group -> group.clients },
                )
            }

            is ClientOnVolumeChanged -> {
                val clients = snapserverGroups.map { client ->
                    if (client.id == notification.params.clientId) {
                        client.copy(
                            config = client.config.copy(volume = notification.params.volume),
                        )
                    } else {
                        client
                    }
                }
                _snapserverGroups.clear()
                _snapserverGroups.addAll(clients)
            }

            else -> { }
        }
    }

    fun onClientVolumeChange(clientId: String, muted: Boolean, volume: Int) {
        viewModelScope.launch {
            snapcastControlClient.sendSetVolume(clientId, muted, volume)
        }
    }

    fun unbindBroadcasterService() {
        if (mBound) {
            appContext.unbindService(serviceConnection)
            mBound = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        unbindBroadcasterService()
    }

    companion object {
        private val TAG = BroadcasterViewModel::class.simpleName
    }
}
