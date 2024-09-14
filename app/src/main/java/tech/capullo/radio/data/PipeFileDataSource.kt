package tech.capullo.radio.data

import android.content.Context
import android.system.Os.mkfifo
import android.system.OsConstants.S_IRUSR
import android.system.OsConstants.S_IWUSR
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class PipeFileDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val CACHE_DIR = context.cacheDir
    private val NATIVE_LIB_DIR_PATH = context.applicationInfo.nativeLibraryDir

    fun getPipeFilepath(): String? {
        val pipeFile = File(CACHE_DIR, PIPE_NAME)

        if (pipeFile.exists()) {
            Log.d(TAG, "Deleting existing PIPE file")
            pipeFile.delete()
        }

        Log.d(TAG, "Creating PIPE: ${pipeFile.absolutePath}")
        try {
            mkfifo(pipeFile.absolutePath, S_IRUSR or S_IWUSR)
            return pipeFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error creating PIPE file: ${e.message}")
            return null
        }
    }

    fun getNativeLibDirPath(): String {
        return NATIVE_LIB_DIR_PATH
    }

    fun getCacheDirPath(): String {
        return CACHE_DIR.absolutePath
    }

    companion object {
        private const val TAG = "RadioRepository"
        private const val PIPE_NAME = "filifo"
    }
}