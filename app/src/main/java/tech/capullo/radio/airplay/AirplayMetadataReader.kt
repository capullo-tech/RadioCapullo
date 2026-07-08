package tech.capullo.radio.airplay

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.snapcast.StreamProperties
import java.io.FileInputStream
import java.io.InputStreamReader
import javax.inject.Inject

/**
 * Reads shairport-sync's metadata FIFO and exposes the parsed AirPlay
 * [StreamProperties] (title/artist/album/cover-art + play state) as a
 * [StateFlow] for the Airplay Snapcast control bridge to push to clients.
 *
 * Like [AirplayProcess]/SnapserverProcess this is a supervised run loop: opening
 * the FIFO O_RDONLY blocks until shairport-sync opens the write end (same
 * rendezvous as the audio pipe), and a returned [run] (writer closed -> EOF)
 * lets the service's supervisor reopen it for the next session.
 */
class AirplayMetadataReader @Inject constructor(private val radioRepository: RadioRepository) {
    private val parser = AirplayMetadataParser { Base64.decode(it, Base64.DEFAULT) }

    private val _properties = MutableStateFlow(parser.snapshot())
    val properties: StateFlow<StreamProperties> = _properties.asStateFlow()

    private val _credentials = MutableStateFlow(parser.credentials())
    val credentials: StateFlow<DacpCredentials?> = _credentials.asStateFlow()

    suspend fun run() = coroutineScope {
        val pipePath = radioRepository.getAirplayMetadataPipeFilepath()
        if (pipePath == null) {
            Log.e(TAG, "No AirPlay metadata PIPE; not reading shairport metadata")
            return@coroutineScope
        }

        // Drop any partial item buffered from a previous (closed) session.
        parser.resetBuffer()

        // Blocks until shairport-sync opens the write end.
        FileInputStream(pipePath).use { fis ->
            val reader = InputStreamReader(fis, Charsets.ISO_8859_1)
            val buf = CharArray(BUFFER_SIZE)
            var read: Int
            while (reader.read(buf).also { read = it } != -1) {
                currentCoroutineContext().ensureActive()
                if (!isActive) break
                parser.feed(String(buf, 0, read))?.let {
                    _properties.value = it
                    _credentials.value = parser.credentials()
                }
            }
        }
    }

    companion object {
        private val TAG = AirplayMetadataReader::class.java.simpleName
        private const val BUFFER_SIZE = 4096
    }
}
