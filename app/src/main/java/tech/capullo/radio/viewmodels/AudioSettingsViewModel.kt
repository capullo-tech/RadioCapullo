package tech.capullo.radio.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.capullo.radio.data.AudioSettingsRepository
import tech.capullo.radio.ui.AudioSettings
import javax.inject.Inject

@HiltViewModel
class AudioSettingsViewModel @Inject constructor(private val repository: AudioSettingsRepository) :
    ViewModel() {

    val audioSettings = repository.getAudioSettings()

    fun saveAudioSettings(settings: AudioSettings) {
        repository.saveAudioSettings(settings)
    }
}
