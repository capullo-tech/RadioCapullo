package tech.capullo.radio

import android.app.Application
import android.util.Log
import xyz.gianlu.librespot.audio.decoders.Decoders
import xyz.gianlu.librespot.audio.format.SuperAudioFormat
import xyz.gianlu.librespot.player.decoders.AndroidNativeDecoder

class RadioCapulloApp : Application() {
    private val TAG = RadioCapulloApp::class.java.simpleName

    init {
        Log.d(TAG, "Application subclass INIT")
        //Decoders.registerDecoder(SuperAudioFormat.VORBIS, AndroidNativeDecoder::class.java)
        //Decoders.registerDecoder(SuperAudioFormat.MP3, AndroidNativeDecoder::class.java)
    }
}