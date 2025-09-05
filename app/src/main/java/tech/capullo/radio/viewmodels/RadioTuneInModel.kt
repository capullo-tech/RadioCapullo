package tech.capullo.radio.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.capullo.radio.services.SnapclientService
import tech.capullo.radio.ui.AudioChannel
import javax.inject.Inject

data class ServiceConnectionState(
    val isConnected: Boolean = false,
    val serverIp: String = "",
    val channel: AudioChannel = AudioChannel.STEREO,
)

@HiltViewModel
class RadioTuneInModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
) : ViewModel() {

    private var binder: SnapclientService.SnapclientBinder? = null
    private var isBound = false

    private val _connectionState = MutableStateFlow(ServiceConnectionState())
    val connectionState: StateFlow<ServiceConnectionState> = _connectionState.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as SnapclientService.SnapclientBinder
            isBound = true

            // collect both server ip and audio channel settings
            viewModelScope.launch {
                launch {
                    binder?.getSnapserverIpFlow()?.collect { snapserverIp ->
                        _connectionState.value = connectionState.value.copy(
                            serverIp = snapserverIp,
                        )
                    }
                }
                launch {
                    binder?.getAudioChannelFlow()?.collect { audioChannel ->
                        _connectionState.value = connectionState.value.copy(
                            channel = audioChannel,
                        )
                    }
                }
            }

            _connectionState.value = connectionState.value.copy(
                isConnected = isBound,
            )
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
            isBound = false

            _connectionState.value = connectionState.value.copy(
                isConnected = isBound,
            )
        }
    }

    init {
        // upon starting, try to bind to the service if it is already running
        // service connection callback will trigger, changing the UI state
        Intent(applicationContext, SnapclientService::class.java).also { intent ->
            applicationContext.bindService(intent, serviceConnection, 0)
        }
    }

    private fun getSharedPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)

    fun saveLastServerText(text: String) {
        getSharedPreferences(applicationContext).edit {
            putString("my_text", text)
        }
    }

    fun getLastServerText(): String =
        getSharedPreferences(applicationContext).getString("my_text", "") ?: ""

    fun startSnapclientService(ip: String, audioChannel: AudioChannel) {
        // UDF -> set the server IP and Audio Channel settings onto the Service, collect the values
        // as a flow to then display on the UI
        val intent = Intent(applicationContext, SnapclientService::class.java).apply {
            putExtra(SnapclientService.KEY_IP, ip)
            putExtra(SnapclientService.KEY_AUDIO_CHANNEL, audioChannel.ordinal)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }

        applicationContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun updateAudioChannel(channel: AudioChannel) {
        binder?.updateAudioChannel(channel)
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            applicationContext.unbindService(serviceConnection)
            isBound = false
            binder = null
        }
    }
}
