package tech.capullo.radio

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {
    private val TAG = MainApplication::class.java.simpleName

    init {
        Log.d(TAG, "Application subclass INIT")
        //Decoders.registerDecoder(SuperAudioFormat.VORBIS, AndroidNativeDecoder::class.java)
        //Decoders.registerDecoder(SuperAudioFormat.MP3, AndroidNativeDecoder::class.java)
    }
}