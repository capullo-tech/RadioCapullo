package tech.capullo.radio.data

import android.content.Context
import android.system.Os.mkfifo
import android.system.OsConstants.S_IRUSR
import android.system.OsConstants.S_IWUSR
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class ConfFileDataSource @Inject constructor(@ApplicationContext private val appContext: Context) {
    fun getPipeFilepath(): String? {
        val pipeFile = File(getCacheDir(), PIPE_NAME)

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

    fun getNativeLibDirPath(): String = appContext.applicationInfo.nativeLibraryDir

    fun getCacheDir(): File = appContext.cacheDir

    fun getFilesDir(): File = appContext.filesDir

    // Add empty/dummy snapserver conf file, all settings are specified as cli args on the
    // SnapserverProcess
    fun getSnapserverConfPath(): String {
        val confFile = File(getCacheDir(), "snapserver.conf")

        if (!confFile.exists()) {
            try {
                confFile.createNewFile()
                Log.d(TAG, "Created snapserver.conf: ${confFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating snapserver.conf: ${e.message}")
            }
        }

        return confFile.absolutePath
    }

    companion object {
        private val TAG = ConfFileDataSource::class.java.simpleName
        private const val PIPE_NAME = "filifo"
    }
}
