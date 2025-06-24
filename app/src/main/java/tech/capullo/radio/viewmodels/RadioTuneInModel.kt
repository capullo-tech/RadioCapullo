package tech.capullo.radio.viewmodels

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.capullo.radio.compose.AudioChannel
import tech.capullo.radio.services.SnapclientService
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

    fun startSnapclientService(ip: String, audioChannel: AudioChannel) {
        val intent = Intent(applicationContext, SnapclientService::class.java).apply {
            putExtra(SnapclientService.KEY_IP, ip)
            putExtra(SnapclientService.KEY_AUDIO_CHANNEL, audioChannel.ordinal)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
    }
}
