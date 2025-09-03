package tech.capullo.radio.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
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
import tech.capullo.radio.espoti.EspotiSessionRepository
import tech.capullo.radio.services.RadioBroadcasterService
import tech.capullo.radio.snapcast.Client
import tech.capullo.radio.snapcast.SnapcastControlClient
import javax.inject.Inject

sealed interface RadioBroadcasterUiState {

    data class EspotiPlayerReady(
        val hostAddresses: List<String>,
        val snapcastClients: List<Client>,
    ) : RadioBroadcasterUiState

    data class EspotiConnect(val isLoading: Boolean, val deviceName: String) :
        RadioBroadcasterUiState
}

private data class RadioBroadcasterViewModelState(
    val isPlaybackReady: Boolean = false,
    val isLoading: Boolean = true,
    val deviceName: String = "",
    val snapcastClients: List<Client> = emptyList(),
    val hostAddresses: List<String> = emptyList(),
) {
    /**
     * Converts this [RadioBroadcasterViewModelState] state into a strongly typed
     * [RadioBroadcasterUiState] for driving the UI.
     */
    fun toUiState(): RadioBroadcasterUiState = if (!isPlaybackReady) {
        RadioBroadcasterUiState.EspotiConnect(
            isLoading = isLoading,
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
    private val espotiSessionRepository: EspotiSessionRepository,
) : ViewModel() {

    /**
     * The initial state of the ViewModel.
     *
     * Initially check for previous session, display a loading screen in the meanwhile.
     * In case no previous session is found, the UI will show a screen to connect to the device as a
     * speaker.
     */
    private val viewModelState = MutableStateFlow(
        RadioBroadcasterViewModelState(
            isPlaybackReady = false,
            isLoading = true,
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

    class RadioServiceWrapper(service: RadioBroadcasterService) {
        val isPlayerLoading = service.isPlayerLoading
    }

    private var serviceWrapper: RadioServiceWrapper? = null
    private var mBound: Boolean = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RadioBroadcasterService.LocalBinder
            serviceWrapper = RadioServiceWrapper(binder.getService())
            mBound = true

            viewModelScope.launch {
                serviceWrapper?.isPlayerLoading?.collect { isLoading ->
                    // TODO: (potentially) display a screen saying the sessions is established
                    // and the player is loading
                    if (!isLoading) {
                        val hostAddresses = repository.getInetAddresses()
                        viewModelState.value =
                            viewModelState.value.copy(
                                isPlaybackReady = true,
                                hostAddresses = hostAddresses,
                                snapcastClients = snapcastClients.value,
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

    /**
     * Both [RadioBroadcasterViewModel] and [RadioBroadcasterService] will observe the session state.
     * When:
     * 1. EspotiSessionRepository.SessionState.Created -> RadioBroadcasterService ->
     *    serviceWrapper?.isPlayerLoading?.isLoading -> responds -> UI updates
     * 2. EspotiSessionRepository.SessionState.Error -> RadioBroadcasterViewModel ->
     *    startEspotiNsd
     */
    init {
        startBroadcasterService()

        viewModelScope.launch {
            launch {
                espotiSessionRepository.createSessionWithStoredCredentials()
            }
            launch {
                espotiSessionRepository.sessionState.collect {
                    it?.let { sessionState ->
                        if (sessionState is EspotiSessionRepository.SessionState.Error) {
                            startEspotiNsd()
                            viewModelState.value =
                                viewModelState.value.copy(
                                    isPlaybackReady = false,
                                    isLoading = false,
                                )
                        }
                    }
                }
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

        applicationContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
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
            applicationContext.unbindService(serviceConnection)
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
