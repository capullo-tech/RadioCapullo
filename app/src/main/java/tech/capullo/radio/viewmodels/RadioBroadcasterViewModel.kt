package tech.capullo.radio.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.toMutableStateList
import androidx.core.os.HandlerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.capullo.radio.data.Client
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.data.SnapcastControlClient
import tech.capullo.radio.espoti.EspotiConnectHandlerImpl.SessionParams
import tech.capullo.radio.espoti.EspotiNsdManager
import tech.capullo.radio.services.RadioBroadcasterService
import javax.inject.Inject

@HiltViewModel
class RadioBroadcasterViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val repository: RadioRepository,
    private val espotiNsdManager: EspotiNsdManager,
) : ViewModel() {
    private val _snapcastClients = MutableStateFlow<List<Client>>(emptyList())
    val snapcastClients = _snapcastClients.asStateFlow()

    private val _hostAddresses = repository.getInetAddresses().toMutableStateList()

    val hostAddresses: List<String>
        get() = _hostAddresses

    fun getDeviceName(): String = repository.getDeviceName()

    val mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper())

    class RadioServiceWrapper(private val service: RadioBroadcasterService) {
        fun createSessionAndPlayer(sessionParams: SessionParams, deviceName: String) {
            service.createSessionAndPlayer(sessionParams, deviceName)
        }
    }

    private var serviceWrapper: RadioServiceWrapper? = null
    private var mBound: Boolean = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RadioBroadcasterService.LocalBinder
            serviceWrapper = RadioServiceWrapper(binder.getService())
            mBound = true

            startEspotiNsd()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceWrapper = null
            mBound = false
        }
    }

    init {
        startBroadcasterService()
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

    fun startEspotiNsd() {
        viewModelScope.launch(Dispatchers.IO) {
            espotiNsdManager.start()?.let { sessionParams ->
                mainThreadHandler.post {
                    if (mBound) {
                        serviceWrapper?.createSessionAndPlayer(sessionParams, getDeviceName())
                        startSnapcastControlClient()
                    }
                }
            }
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
