package tech.capullo.radio.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import tech.capullo.radio.services.SnapclientService
import tech.capullo.radio.snapcast.DiscoveredSnapserver
import tech.capullo.radio.snapcast.SnapserverDiscoveryManager
import tech.capullo.radio.ui.model.AudioChannel
import javax.inject.Inject

data class TuneInState(
    val availableServers: List<DiscoveredSnapserver> = emptyList(),
    val serverIp: String = "",
    val audioChannel: AudioChannel = AudioChannel.STEREO,
    val isTunedIn: Boolean = false,
)

val Context.dataStore by preferencesDataStore(name = "tune_in_prefs")

@HiltViewModel
class TuneInModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val discoveryManager: SnapserverDiscoveryManager,
) : ViewModel() {

    object PreferencesKeys {
        val LAST_SERVER_TEXT = stringPreferencesKey("last_server_text")
    }

    private var binder: SnapclientService.SnapclientBinder? = null
    private var isBound = false

    val lastServerTextFlow: Flow<String> = applicationContext.dataStore.data
        .catch { exception ->
            if (exception is Exception) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            println("gitting the last server")
            preferences[PreferencesKeys.LAST_SERVER_TEXT] ?: ""
        }

    private val _tuneInState = MutableStateFlow(TuneInState())
    val tuneInState = _tuneInState.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as SnapclientService.SnapclientBinder
            isBound = true

            // collect both server ip and audio channel settings
            viewModelScope.launch {
                launch {
                    binder?.getSnapserverIpFlow()?.collect { snapserverIp ->
                        _tuneInState.value = tuneInState.value.copy(
                            serverIp = snapserverIp,
                        )
                    }
                }
                launch {
                    binder?.getAudioChannelFlow()?.collect { audioChannel ->
                        _tuneInState.value = tuneInState.value.copy(
                            audioChannel = audioChannel,
                        )
                    }
                }
            }

            _tuneInState.value = tuneInState.value.copy(
                isTunedIn = isBound,
            )
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
            isBound = false

            _tuneInState.value = tuneInState.value.copy(
                isTunedIn = isBound,
            )
        }
    }

    init {
        // upon starting, try to bind to the service if it is already running
        // service connection callback will trigger, changing the UI state
        Intent(applicationContext, SnapclientService::class.java).also { intent ->
            applicationContext.bindService(intent, serviceConnection, 0)
        }

        viewModelScope.launch {
            launch {
                // collect saved preferences for previously connected servers
                lastServerTextFlow.collect {
                    println("collected previously saved value")
                    _tuneInState.value = _tuneInState.value.copy(serverIp = it)
                }
            }
            launch {
                // update discovered server list
                discoveryManager.discoveredServices.collect {
                    _tuneInState.value = _tuneInState.value.copy(availableServers = it)
                }
            }
        }

        // Start discovering snapcast services
        discoveryManager.startDiscovery()
    }

    suspend fun saveLastServerText(text: String) {
        applicationContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SERVER_TEXT] = text
        }
    }

    fun onServerIPTextFieldValueChanged(serverIpText: String) {
        _tuneInState.value = _tuneInState.value.copy(serverIp = serverIpText)
    }

    fun startSnapclientService() {
        // UDF -> set the server IP and Audio Channel settings onto the Service, collect the values
        // as a flow to then display on the UI
        val intent = Intent(applicationContext, SnapclientService::class.java).apply {
            putExtra(SnapclientService.KEY_IP, tuneInState.value.serverIp)
            putExtra(SnapclientService.KEY_AUDIO_CHANNEL, tuneInState.value.audioChannel.ordinal)
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
        discoveryManager.stopDiscovery()
    }
}
