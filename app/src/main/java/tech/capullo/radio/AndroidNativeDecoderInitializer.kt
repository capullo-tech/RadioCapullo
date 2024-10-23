package tech.capullo.radio

import android.content.Context
import androidx.startup.Initializer
import tech.capullo.radio.data.AndroidNativeDecoder
import xyz.gianlu.librespot.audio.decoders.Decoders
import xyz.gianlu.librespot.audio.format.SuperAudioFormat

class AndroidNativeDecoderInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        // This initializer doesn't return anything (Unit)
        Decoders.registerDecoder(SuperAudioFormat.VORBIS, 0, AndroidNativeDecoder::class.java)
        Decoders.registerDecoder(SuperAudioFormat.MP3, 0, AndroidNativeDecoder::class.java)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        // Define dependencies if needed
        return emptyList()
    }
}
