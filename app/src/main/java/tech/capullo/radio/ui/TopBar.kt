package tech.capullo.radio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioTopBar(title: String, onSettingsClick: () -> Unit, modifier: Modifier = Modifier) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = modifier,
    )
}

data class AudioSettings(
    val audioChannel: AudioChannel = AudioChannel.STEREO,
    val latency: Int = 0,
    val volume: Float = 1.0f,
    val persistSettings: Boolean = false,
)

@Composable
fun AudioSettingsDialog(
    currentSettings: AudioSettings,
    onSettingsChanged: (AudioSettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedChannel by remember { mutableStateOf(currentSettings.audioChannel) }
    var latency by remember { mutableStateOf(currentSettings.latency.toString()) }
    var volume by remember { mutableFloatStateOf(currentSettings.volume) }
    var persistSettings by remember { mutableStateOf(currentSettings.persistSettings) }

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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Audio Channel Selection
                Text(
                    text = "Audio Channel:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AudioChannel.entries.forEach { channel ->
                        ToggleButton(
                            checked = selectedChannel == channel,
                            onCheckedChange = { if (it) selectedChannel = channel },
                            modifier = Modifier.weight(channel.modifierWeight),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) {
                            Icon(
                                if (selectedChannel == channel) {
                                    channel.selectedIcon
                                } else {
                                    channel.unselectedIcon
                                },
                                contentDescription = channel.label,
                            )
                            Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                            Text(text = channel.label)
                        }
                    }
                }

                // Latency Setting (placeholder)
                OutlinedTextField(
                    value = latency,
                    onValueChange = { newValue -> latency = newValue },
                    label = { Text("Latency (ms)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0") },
                )

                // Volume Setting
                Text(
                    text = "Volume: ${(volume * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )

                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Persistence Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = persistSettings,
                        onCheckedChange = { persistSettings = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save settings for next app start",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newSettings = AudioSettings(
                        audioChannel = selectedChannel,
                        latency = latency.toIntOrNull() ?: 0,
                        volume = volume,
                        persistSettings = persistSettings,
                    )
                    onSettingsChanged(newSettings)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier,
    )
}
