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

    // Idempotent, unlike getPipeFilepath(): both snapserver (reader) and
    // shairport-sync (writer) resolve this path independently, so recreating
    // the FIFO here could leave them attached to different inodes.
    fun getAirplayPipeFilepath(): String? {
        val pipeFile = File(getCacheDir(), AIRPLAY_PIPE_NAME)

        if (pipeFile.exists()) {
            return pipeFile.absolutePath
        }

        Log.d(TAG, "Creating AirPlay PIPE: ${pipeFile.absolutePath}")
        try {
            mkfifo(pipeFile.absolutePath, S_IRUSR or S_IWUSR)
            return pipeFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error creating AirPlay PIPE file: ${e.message}")
            return null
        }
    }

    // Metadata FIFO shairport-sync writes its <item> stream to (title/artist/
    // album/cover art + play state). Idempotent for the same inode reason as
    // getAirplayPipeFilepath(): shairport-sync (writer) and AirplayMetadataReader
    // (reader) open this path independently.
    fun getAirplayMetadataPipeFilepath(): String? {
        val pipeFile = File(getCacheDir(), AIRPLAY_METADATA_PIPE_NAME)

        if (pipeFile.exists()) {
            return pipeFile.absolutePath
        }

        Log.d(TAG, "Creating AirPlay metadata PIPE: ${pipeFile.absolutePath}")
        try {
            mkfifo(pipeFile.absolutePath, S_IRUSR or S_IWUSR)
            return pipeFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error creating AirPlay metadata PIPE file: ${e.message}")
            return null
        }
    }

    // shairport-sync settings are given via its libconfig-style conf file,
    // rewritten on each start since the device name can change. When
    // metadataPipeFilepath is non-null, shairport-sync (built with
    // CONFIG_METADATA) emits its <item> metadata stream to that FIFO for
    // AirplayMetadataReader to consume.
    fun getShairportConfPath(
        deviceName: String,
        pipeFilepath: String,
        metadataPipeFilepath: String? = null,
    ): String {
        val confFile = File(getCacheDir(), "shairport-sync.conf")
        val escapedName = deviceName.replace("\\", "\\\\").replace("\"", "\\\"")
        val metadataBlock = if (metadataPipeFilepath != null) {
            """

            metadata = {
              enabled = "yes";
              include_cover_art = "yes";
              pipe_name = "$metadataPipeFilepath";
            };
            """.trimIndent()
        } else {
            ""
        }
        confFile.writeText(
            """
            general = {
              name = "$escapedName";
              output_backend = "pipe";
            };
            pipe = {
              name = "$pipeFilepath";
            };
            """.trimIndent() + metadataBlock,
        )
        Log.d(TAG, "Wrote shairport-sync.conf: ${confFile.absolutePath}")
        return confFile.absolutePath
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
        private const val AIRPLAY_PIPE_NAME = "airplay-fifo"
        private const val AIRPLAY_METADATA_PIPE_NAME = "airplay-metadata-fifo"
    }
}
