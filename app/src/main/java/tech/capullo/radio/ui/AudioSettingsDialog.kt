package tech.capullo.radio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class AudioChannel(val label: String) {
    LEFT("Left"),
    STEREO("Stereo"),
    RIGHT("Right"),
}

data class AudioSettings(
    val audioChannel: AudioChannel = AudioChannel.STEREO,
    val latency: Int = 0,
    val volume: Float = 1.0f,
    val persistSettings: Boolean = true,
)

@Composable
fun AudioSettingsDialog(
    currentSettings: AudioSettings,
    onSettingsChanged: (AudioSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    var audioChannel by remember { mutableStateOf(currentSettings.audioChannel) }
    var latency by remember { mutableIntStateOf(currentSettings.latency) }
    var volume by remember { mutableFloatStateOf(currentSettings.volume) }
    var persistSettings by remember { mutableStateOf(currentSettings.persistSettings) }

    // Update local state when currentSettings changes
    LaunchedEffect(currentSettings) {
        audioChannel = currentSettings.audioChannel
        latency = currentSettings.latency
        volume = currentSettings.volume
        persistSettings = currentSettings.persistSettings
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Audio Settings",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Audio Channel Selection
                Text(
                    text = "Audio Channel",
                    style = MaterialTheme.typography.titleMedium,
                )
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AudioChannel.entries.forEach { channel ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (audioChannel == channel),
                                    onClick = {
                                        val newChannel = channel
                                        audioChannel = newChannel
                                        val newSettings = AudioSettings(
                                            audioChannel = newChannel,
                                            latency = latency,
                                            volume = volume,
                                            persistSettings = persistSettings,
                                        )
                                        onSettingsChanged(newSettings)
                                    },
                                    role = Role.RadioButton,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = (audioChannel == channel),
                                onClick = null,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = channel.label,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }

                // Latency Setting
                Text(
                    text = "Latency (ms): $latency",
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    value = latency.toString(),
                    onValueChange = { newValue ->
                        val newLatency = newValue.toIntOrNull() ?: 0
                        latency = newLatency
                        val newSettings = AudioSettings(
                            audioChannel = audioChannel,
                            latency = newLatency,
                            volume = volume,
                            persistSettings = persistSettings,
                        )
                        onSettingsChanged(newSettings)
                    },
                    label = { Text("Latency") },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Volume Setting
                Text(
                    text = "Volume: ${(volume * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    value = volume,
                    onValueChange = { newVolume ->
                        volume = newVolume
                        val newSettings = AudioSettings(
                            audioChannel = audioChannel,
                            latency = latency,
                            volume = newVolume,
                            persistSettings = persistSettings,
                        )
                        onSettingsChanged(newSettings)
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Persist Settings Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Persist Settings",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Switch(
                        checked = persistSettings,
                        onCheckedChange = { newPersistSettings ->
                            persistSettings = newPersistSettings
                            val newSettings = AudioSettings(
                                audioChannel = audioChannel,
                                latency = latency,
                                volume = volume,
                                persistSettings = newPersistSettings,
                            )
                            onSettingsChanged(newSettings)
                        },
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
