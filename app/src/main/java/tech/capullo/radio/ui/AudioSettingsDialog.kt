package tech.capullo.radio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.capullo.radio.ui.model.AudioChannel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AudioSettingsDialog(
    onDismissRequest: () -> Unit,
    selectedChannel: AudioChannel,
    onCheckedChanged: (Boolean, AudioChannel) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Audio Channel Settings") },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    ButtonGroupDefaults.ConnectedSpaceBetween,
                ),
            ) {
                AudioChannel.entries.forEach { channel ->
                    ToggleButton(
                        checked = selectedChannel == channel,
                        onCheckedChange = { isChecked -> onCheckedChanged(isChecked, channel) },
                        modifier = Modifier.Companion.weight(channel.modifierWeight),
                        shapes =
                        when (channel) {
                            AudioChannel.LEFT -> {
                                ButtonGroupDefaults.connectedLeadingButtonShapes()
                            }

                            AudioChannel.RIGHT -> {
                                ButtonGroupDefaults.connectedTrailingButtonShapes()
                            }

                            AudioChannel.STEREO -> {
                                ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                        },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Icon(
                            if (selectedChannel == channel) {
                                channel.selectedIcon
                            } else {
                                channel.unselectedIcon
                            },
                            contentDescription = channel.label,
                        )
                        Spacer(Modifier.Companion.size(ToggleButtonDefaults.IconSpacing))
                        Text(
                            text = channel.label,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Dismiss")
            }
        },
    )
}
