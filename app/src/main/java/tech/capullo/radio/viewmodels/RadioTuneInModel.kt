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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import tech.capullo.radio.services.SnapclientService
import tech.capullo.radio.ui.AudioChannel
import javax.inject.Inject

@HiltViewModel
class RadioTuneInModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
) : ViewModel() {

    private fun getSharedPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)

    fun saveLastServerText(text: String) {
        getSharedPreferences(applicationContext).edit {
            putString("my_text", text)
        }
    }

    fun getLastServerText(): String =
        getSharedPreferences(applicationContext).getString("my_text", "") ?: ""

    private var ip: String = "localhost"

    private var mBound: Boolean = false
    private var binder: SnapclientService.LocalBinder? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            binder = service as? SnapclientService.LocalBinder
            binder?.run { mBound = true }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
        }
    }

    fun startSnapclientService(ip: String, audioChannel: AudioChannel) {
        this.ip = ip
        val intent = Intent(applicationContext, SnapclientService::class.java).apply {
            putExtra(SnapclientService.KEY_IP, ip)
            putExtra(SnapclientService.KEY_AUDIO_CHANNEL, audioChannel.ordinal)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
        applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun launchSnapclient(audioChannel: AudioChannel) {
        binder?.launchSnapclient(ip, audioChannel.ordinal)

        viewModelScope.launch {
            // TODO: follow up with an UI state to the channel changing 中
        }
    }
}
