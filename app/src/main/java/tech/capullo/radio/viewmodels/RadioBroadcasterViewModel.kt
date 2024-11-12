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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.espoti.EspotiNsdManager
import tech.capullo.radio.espoti.EspotiZeroconfServer
import tech.capullo.radio.espoti.EspotiZeroconfServer.SessionParams
import tech.capullo.radio.services.RadioBroadcasterService
import xyz.gianlu.librespot.core.Session
import javax.inject.Inject

@HiltViewModel
class RadioBroadcasterViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val repository: RadioRepository,
    private val espotiNsdManager: EspotiNsdManager
) : ViewModel() {
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
        startNsdService()
    }

    fun startNsdService() {
        startBroadcasterService()

        val sessionListener = object : EspotiZeroconfServer.SessionListener {
            override fun sessionClosing(session: Session) {
                session.close()
            }

            override fun sessionChanged(sessionParams: SessionParams) {
                Log.d(TAG, "Session changed on thread: ${Thread.currentThread().name}")
                mainThreadHandler.post {
                    mService.startLibrespot(sessionParams)
                }
            }
        }

        espotiNsdManager.start(getDeviceName(), sessionListener)
    }

    fun startBroadcasterService() {
        // start the broadcaster service
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
