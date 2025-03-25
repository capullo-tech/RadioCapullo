package tech.capullo.radio.espoti

import android.content.Context
import androidx.core.content.edit
import com.spotify.connectstate.Connect
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.capullo.radio.data.RadioRepository
import xyz.gianlu.librespot.common.Utils
import xyz.gianlu.librespot.core.Session
import java.io.File
import java.security.SecureRandom
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EspotiSessionManager @Inject constructor(
    @ApplicationContext val appContext: Context,
    radioRepository: RadioRepository,
) {
    private var _session: Session? = null
    val session get() = _session ?: throw IllegalStateException("Session is not created yet!")

    // Session configuration properties
    val espotiDeviceName: String = "RadioCapullo - ${radioRepository.getDeviceName()}"
    val espotiDeviceType: Connect.DeviceType = Connect.DeviceType.SPEAKER
    val espotiDeviceId: String = loadEspotiDeviceID()

    fun loadEspotiDeviceID(): String {
        val sharedPreferences = appContext.getSharedPreferences(
            ESPOTI_PREFENCE_NAME,
            Context.MODE_PRIVATE,
        )
        var espotiDeviceId = sharedPreferences.getString(ESPOTI_DEVICE_ID_PREFENCE, null)

        if (espotiDeviceId == null) {
            // Generate a new device ID
            espotiDeviceId =
                Utils.randomHexString(
                    SecureRandom(),
                    ESPOTI_DEVICE_ID_LENGTH,
                ).lowercase(Locale.getDefault())

            // Save it for future use
            sharedPreferences.edit { putString(ESPOTI_DEVICE_ID_PREFENCE, espotiDeviceId) }
        }

        return espotiDeviceId
    }

    fun createSession(): Session.Builder = Session.Builder(createSessionConfig())
        .setDeviceType(espotiDeviceType)
        .setDeviceName(espotiDeviceName)
        .setDeviceId(espotiDeviceId)
        .setPreferredLocale(Locale.getDefault().language)

    private fun createSessionConfig() = Session.Configuration.Builder()
        .setCacheEnabled(true)
        .setDoCacheCleanUp(true)
        .setCacheDir(File(appContext.cacheDir, ESPOTI_CACHE_DIR))
        .setStoredCredentialsFile(File(appContext.filesDir, ESPOTI_CREDENTIALS_FILE))
        .build()

    fun setSession(s: Session) {
        _session = s
    }

    companion object {
        private const val ESPOTI_DEVICE_ID_PREFENCE = "espoti_device_id"
        private const val ESPOTI_PREFENCE_NAME = "espoti_preferences"
        private const val ESPOTI_CACHE_DIR = "espoti_cache"
        private const val ESPOTI_CREDENTIALS_FILE = "espoti_creds"

        private const val ESPOTI_DEVICE_ID_LENGTH = 40
    }
}
