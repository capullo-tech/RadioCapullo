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

    private lateinit var mService: RadioBroadcasterService
    private var mBound: Boolean = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected")
            val binder = service as RadioBroadcasterService.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            mBound = false
        }
    }

    init {
        startBroadcasterService()
        viewModelScope.launch(Dispatchers.IO) {
            espotiNsdManager.start()?.let { sessionParams ->
                mainThreadHandler.post {
                    if (mBound) {
                        println("Service is already bound")
                        mService.createSessionAndPlayer(sessionParams, getDeviceName())
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val snapcastControlClient = SnapcastControlClient("127.0.0.1")
            snapcastControlClient.initWebsocket()
            snapcastControlClient.serverStatus.collect { serverStatus ->
                val clients =
                    serverStatus?.result?.server?.groups?.flatMap { it.clients } ?: emptyList()
                _snapcastClients.value = clients
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
