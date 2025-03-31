package tech.capullo.radio.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.espoti.EspotiNsdManager
import tech.capullo.radio.services.RadioBroadcasterService
import tech.capullo.radio.snapcast.Client
import tech.capullo.radio.snapcast.SnapcastControlClient
import javax.inject.Inject

sealed interface RadioBroadcasterUiState {

    data class EspotiPlayerReady(
        val hostAddresses: List<String>,
        val snapcastClients: List<Client>,
    ) : RadioBroadcasterUiState

    data class EspotiConnect(val deviceName: String) : RadioBroadcasterUiState
}

private data class RadioBroadcasterViewModelState(
    val isEspotiPlayerReady: Boolean = false,
    val deviceName: String = "",
    val snapcastClients: List<Client> = emptyList(),
    val hostAddresses: List<String> = emptyList(),
) {
    /**
     * Converts this [RadioBroadcasterViewModelState] state into a strongly typed
     * [RadioBroadcasterUiState] for driving the UI.
     */
    fun toUiState(): RadioBroadcasterUiState = if (!isEspotiPlayerReady) {
        RadioBroadcasterUiState.EspotiConnect(
            deviceName = deviceName,
        )
    } else {
        RadioBroadcasterUiState.EspotiPlayerReady(
            hostAddresses = hostAddresses,
            snapcastClients = snapcastClients,
        )
    }
}

/**
 * ViewModel for the RadioBroadcaster screen.
 *
 * This ViewModel is responsible for managing the state of the RadioBroadcaster screen and
 * interacting with the RadioRepository and EspotiNsdManager.
 *
 * @param applicationContext The application context.
 * @param repository The RadioRepository instance.
 * @param espotiNsdManager The EspotiNsdManager instance.
 */
@HiltViewModel
class RadioBroadcasterViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val repository: RadioRepository,
    private val espotiNsdManager: EspotiNsdManager,
) : ViewModel() {

    private val viewModelState = MutableStateFlow(
        RadioBroadcasterViewModelState(
            isEspotiPlayerReady = false,
            deviceName = repository.getDeviceName(),
            snapcastClients = emptyList(),
            hostAddresses = emptyList(),
        ),
    )

    // UI state exposed to the UI
    val uiState = viewModelState
        .map(RadioBroadcasterViewModelState::toUiState)
        .stateIn(
            viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = viewModelState.value.toUiState(),
        )

    private val _snapcastClients = MutableStateFlow<List<Client>>(emptyList())
    val snapcastClients = _snapcastClients.asStateFlow()

    private val _hostAddresses = repository.getInetAddresses().toMutableStateList()

    val hostAddresses: List<String>
        get() = _hostAddresses

    fun getDeviceName(): String = repository.getDeviceName()

    class RadioServiceWrapper(service: RadioBroadcasterService) {
        val isPlayerLoading = service.isPlayerLoading
    }

    private var serviceWrapper: RadioServiceWrapper? = null
    private var mBound: Boolean = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RadioBroadcasterService.LocalBinder
            serviceWrapper = RadioServiceWrapper(binder.getService())
            mBound = true

            startEspotiNsd()

            viewModelScope.launch {
                serviceWrapper?.isPlayerLoading?.collect { isLoading ->
                    if (!isLoading) {
                        val hostAddresses = repository.getInetAddresses()
                        viewModelState.value =
                            viewModelState.value.copy(
                                isEspotiPlayerReady = true,
                                hostAddresses = hostAddresses,
                                snapcastClients = _snapcastClients.value,
                            )
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceWrapper = null
            mBound = false
        }
    }

    init {
        startBroadcasterService()
    }

    fun checkEspotiSessionConfigured() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
            }
        }
    }

    fun startBroadcasterService() {
        val intent = Intent(applicationContext, RadioBroadcasterService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
        applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    // Started right after the service is bound
    fun startEspotiNsd() {
        viewModelScope.launch(Dispatchers.IO) {
            espotiNsdManager.start()
        }
    }

    fun startSnapcastControlClient() {
        val snapcastControlClient = SnapcastControlClient("127.0.0.1")

        viewModelScope.launch(Dispatchers.IO) {
            snapcastControlClient.initWebsocket()
        }
        viewModelScope.launch {
            snapcastControlClient.serverStatus.collect { serverStatus ->
                Log.d(TAG, "Snapcast server status update: $serverStatus")
                val clients =
                    serverStatus.result.server.groups.flatMap { it.clients }
                _snapcastClients.value = clients
            }
        }
    }

    fun unbindBroadcasterService() {
        if (mBound) {
            applicationContext.unbindService(connection)
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
