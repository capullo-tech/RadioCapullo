package tech.capullo.radio.espoti

import android.content.Context
import android.os.Build
import androidx.startup.Initializer
import org.slf4j.impl.HandroidLoggerAdapter
import tech.capullo.radio.BuildConfig
import xyz.gianlu.librespot.audio.decoders.Decoders
import xyz.gianlu.librespot.audio.format.SuperAudioFormat

class EspotiInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        // This initializer doesn't return anything (Unit)
        Decoders.registerDecoder(SuperAudioFormat.VORBIS, 0, AndroidNativeDecoder::class.java)
        Decoders.registerDecoder(SuperAudioFormat.MP3, 0, AndroidNativeDecoder::class.java)

        HandroidLoggerAdapter.DEBUG = BuildConfig.DEBUG
        HandroidLoggerAdapter.ANDROID_API_LEVEL = Build.VERSION.SDK_INT
        HandroidLoggerAdapter.APP_NAME = "tech.capullo.radio"
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        // Define dependencies if needed
        return emptyList()
    }
}
