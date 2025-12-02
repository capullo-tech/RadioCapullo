package tech.capullo.radio.viewmodels

import android.content.Context
import android.util.Log
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
import tech.capullo.radio.snapcast.DiscoveredSnapserver
import tech.capullo.radio.snapcast.SnapserverDiscoveryManager
import javax.inject.Inject

data class TuneInState(
    val availableServers: List<DiscoveredSnapserver> = emptyList(),
    val serverIp: String = "",
)

val Context.dataStore by preferencesDataStore(name = "tune_in_prefs")

@HiltViewModel
class TuneInViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val discoveryManager: SnapserverDiscoveryManager,
) : ViewModel() {

    object PreferencesKeys {
        val LAST_SERVER_TEXT = stringPreferencesKey("last_server_text")
    }

    val lastServerTextFlow: Flow<String> = applicationContext.dataStore.data
        .catch { exception ->
            if (exception is Exception) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.LAST_SERVER_TEXT] ?: ""
        }

    private val _tuneInState = MutableStateFlow(TuneInState())
    val tuneInState = _tuneInState.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                // collect saved preferences for previously connected servers
                lastServerTextFlow.collect {
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

    fun onServerIPTextFieldValueChanged(serverIpText: String) {
        _tuneInState.value = _tuneInState.value.copy(serverIp = serverIpText)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "TuneInViewModel onCleared")
        discoveryManager.stopDiscovery()
    }

    companion object {
        private val TAG = TuneInViewModel::class.simpleName
    }
}
