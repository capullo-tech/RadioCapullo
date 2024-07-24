package tech.capullo.radio.data

import android.content.Context
import android.system.Os.mkfifo
import android.system.OsConstants.S_IRUSR
import android.system.OsConstants.S_IWUSR
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val CACHE_DIR = context.cacheDir
    private val NATIVE_LIB_DIR = context.applicationInfo.nativeLibraryDir

    fun createNamedPipeFile(): String? {
        val filifoFile = File(CACHE_DIR, NAMED_FIFO)
        Log.d(TAG, "Creating FIFO: ${filifoFile.absolutePath}")

        if (filifoFile.exists()) {
            Log.d(TAG, "Deleting existing FIFO file")
            filifoFile.delete()
        }

        try {
            mkfifo(filifoFile.absolutePath, S_IRUSR or S_IWUSR)
            return filifoFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error creating FIFO file: ${e.message}")
            return null
        }
    }

    // function to get the native library dir
    fun getNativeLibDir(): String {
        return NATIVE_LIB_DIR
    }

    // function to get the cache dir
    fun getCacheDir(): String {
        return CACHE_DIR.absolutePath
    }

    companion object {
        private const val TAG = "RadioRepository"
        private const val NAMED_FIFO = "filifo"
    }
}