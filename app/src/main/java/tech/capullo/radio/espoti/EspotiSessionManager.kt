package tech.capullo.radio.espoti

import android.content.Context
import com.spotify.connectstate.Connect
import dagger.hilt.android.qualifiers.ApplicationContext
import xyz.gianlu.librespot.core.Session
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EspotiSessionManager @Inject constructor(
    @ApplicationContext val appContext: Context,
) {
    private var _session: Session? = null
    val session get() = _session ?: throw IllegalStateException("Session is not created yet!")

    fun createSession(deviceName: String): Session.Builder =
        Session.Builder(createCfg()).setDeviceType(Connect.DeviceType.SPEAKER).setDeviceName(
            deviceName
        ).setDeviceId(null).setPreferredLocale(Locale.getDefault().language)

    private fun createCfg() =
        Session.Configuration.Builder().setCacheEnabled(true).setDoCacheCleanUp(true)
            .setCacheDir(File(appContext.cacheDir, "spa_cache"))
            .setStoredCredentialsFile(File(appContext.filesDir, "spa_creds"))
            .build()

    fun setSession(s: Session) {
        _session = s
    }
}
