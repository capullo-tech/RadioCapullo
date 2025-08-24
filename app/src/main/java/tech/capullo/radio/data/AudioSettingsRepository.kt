package tech.capullo.radio.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.capullo.radio.ui.AudioChannel
import tech.capullo.radio.ui.AudioSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "audio_settings",
        Context.MODE_PRIVATE,
    )

    fun getAudioSettings(): AudioSettings {
        // Always load the persistSettings flag to know if we should load other settings
        val persistSettings = prefs.getBoolean("persist_settings", true)

        return if (persistSettings) {
            // Load persisted settings
            AudioSettings(
                audioChannel = AudioChannel.entries[
                    prefs.getInt(
                        "audio_channel",
                        AudioChannel.STEREO.ordinal,
                    ),
                ],
                latency = prefs.getInt("latency", 0),
                volume = prefs.getFloat("volume", 1.0f),
                persistSettings = persistSettings,
            )
        } else {
            // Return defaults if persistence is disabled
            AudioSettings(
                audioChannel = AudioChannel.STEREO,
                latency = 0,
                volume = 1.0f,
                persistSettings = persistSettings,
            )
        }
    }

    fun saveAudioSettings(settings: AudioSettings) {
        prefs.edit {
            // Always save the persistSettings flag
            putBoolean("persist_settings", settings.persistSettings)

            // Only save other settings if persistence is enabled
            if (settings.persistSettings) {
                putInt("audio_channel", settings.audioChannel.ordinal)
                putInt("latency", settings.latency)
                putFloat("volume", settings.volume)
            }
        }
    }
}
