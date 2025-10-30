package tech.capullo.radio.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
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
import tech.capullo.radio.ui.model.AudioChannel
import javax.inject.Inject

data class BroadcasterUiState(
    val ipv4AddressesResult: IPv4AddressesResult,
    val audioChannel: AudioChannel,
)

@HiltViewModel
class BroadcasterViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val radioRepository: RadioRepository,
) : ViewModel() {

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
        private const val TAG = "RadioBroadcasterViewModel"
    }
}
