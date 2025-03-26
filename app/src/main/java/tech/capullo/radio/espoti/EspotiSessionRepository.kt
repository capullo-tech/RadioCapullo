package tech.capullo.radio.espoti

import android.content.Context
import androidx.core.content.edit
import com.spotify.connectstate.Connect
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.capullo.radio.data.RadioRepository
import xyz.gianlu.librespot.common.Utils
import xyz.gianlu.librespot.core.Session
import java.io.File
import java.security.SecureRandom
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EspotiSessionRepository @Inject constructor(
    @ApplicationContext val appContext: Context,
    radioRepository: RadioRepository,
) {

    // Session configuration properties
    val espotiDeviceName: String = "RadioCapullo - ${radioRepository.getDeviceName()}"
    val espotiDeviceType: Connect.DeviceType = Connect.DeviceType.SPEAKER
    val espotiDeviceId: String = loadEspotiDeviceID()

    private var _session: Session? = null
    val session get() = _session ?: throw IllegalStateException("Session is not created yet!")

    // Session state management
    private val _sessionStateFlow = MutableStateFlow<SessionState>(SessionState.Idle)
    val sessionStateFlow = _sessionStateFlow.asStateFlow()

    sealed class SessionState {
        object Idle : SessionState()
        object Creating : SessionState()
        data class Created(val session: Session) : SessionState()
        data class Error(val message: String) : SessionState()
    }

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

    suspend fun createAndSetupSession(username: String, decryptedBlob: ByteArray) {
        _sessionStateFlow.value = SessionState.Creating

        try {
            val newSession = createSession()
                .blob(username, decryptedBlob)
                .create()
            setSession(newSession)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create session", e)
        }
    }

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
