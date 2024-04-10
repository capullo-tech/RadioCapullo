package tech.capullo.radio

import android.content.Context
import androidx.startup.Initializer
import xyz.gianlu.librespot.audio.decoders.Decoders
import xyz.gianlu.librespot.audio.format.SuperAudioFormat
import xyz.gianlu.librespot.player.decoders.AndroidNativeDecoder

class AndroidNativeDecoderInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        // This initializer doesn't return anything (Unit)
        Decoders.registerDecoder(SuperAudioFormat.VORBIS, AndroidNativeDecoder::class.java)
        Decoders.registerDecoder(SuperAudioFormat.MP3, AndroidNativeDecoder::class.java)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        // Define dependencies if needed
        return emptyList()
    }
}
