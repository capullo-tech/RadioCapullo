package tech.capullo.radio.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.capullo.radio.espoti.EspotiSessionRepository
import tech.capullo.radio.services.RadioBroadcasterService
import javax.inject.Inject

data class EspotiSessionLoadingUiState(
    val isLoading: Boolean,
    val isPlayerReady: Boolean,
    val deviceName: String,
)

@HiltViewModel
class EspotiSessionLoadingViewModel @Inject constructor(
    @ApplicationContext val appContext: Context,
    private val espotiSessionRepository: EspotiSessionRepository,
) : ViewModel() {

    // Starts in the loading state
    private val _uiState = MutableStateFlow(
        EspotiSessionLoadingUiState(
            isLoading = true,
            isPlayerReady = false,
            deviceName = espotiSessionRepository.espotiDeviceName,
        ),
    )
    val uiState: StateFlow<EspotiSessionLoadingUiState> = _uiState.asStateFlow()

    private var initializeCalled = false

    fun initialize() {
        println("Initialize on: ${Process.myPid()} -  ${Thread.currentThread().name}")
        if (initializeCalled) return
        initializeCalled = true

        startBroadcasterService()

        viewModelScope.launch {
            println("Launch on: ${Process.myPid()} -  ${Thread.currentThread().name}")
            val storedSession = espotiSessionRepository.getStoredSession()
            if (storedSession is EspotiSessionRepository.SessionState.Created) {
                // EspotiSessionRepository.SessionState.Created -> RadioBroadcasterService ->
                // serviceWrapper?.isPlayerLoading?.isLoading -> responds -> UI updates
                espotiSessionRepository.setSession(storedSession.session)
            } else {
                // No stored session: show the Spotify Connect screen. Discovery +
                // login are advertised by RadioBroadcasterService for the life of
                // the service, so a connection can arrive here or after the user
                // proceeds to the broadcast screen without Spotify.
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private var mBound: Boolean = false
    private var mService: RadioBroadcasterService.LocalBinder? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RadioBroadcasterService.LocalBinder
            mBound = true
            mService = binder

            viewModelScope.launch {
                binder.getIsPlayerLoadingFlow().collect { isLoading ->
                    if (!isLoading) {
                        // updating this flag will trigger a backStack push to the player view
                        _uiState.update { it.copy(isPlayerReady = true) }
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
            mService = null
        }
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

    fun unbindBroadcasterService() {
        if (mBound) {
            appContext.unbindService(serviceConnection)
            mBound = false
        }
    }

    override fun onCleared() {
        println("on cleared")
        super.onCleared()
        unbindBroadcasterService()
    }
}
