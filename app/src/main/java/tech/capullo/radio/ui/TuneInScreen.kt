package tech.capullo.radio.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.capullo.radio.ui.model.AudioChannel
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.ui.theme.SchemeChoice
import tech.capullo.radio.ui.theme.Typography
import tech.capullo.radio.viewmodels.RadioTuneInModel

@Composable
fun TuneInScreen(radioTuneInModel: RadioTuneInModel = hiltViewModel(), onConnected: () -> Unit) {
    var lastServerText by remember {
        mutableStateOf(radioTuneInModel.getLastServerText())
    }
    var selectedChannel by rememberSaveable { mutableStateOf(AudioChannel.STEREO) }

    // Collect connection state from ViewModel
    val connectionState by radioTuneInModel.connectionState.collectAsState()

    // Navigate when service is connected and running
    if (connectionState.isConnected &&
        connectionState.serverIp.isNotEmpty()
    ) {
        onConnected()
    }

    Scaffold { innerPadding ->
        TuneInScreenContent(
            modifier = Modifier
                .padding(innerPadding),
            lastServerText = lastServerText,
            isButtonEnabled = !connectionState.isConnected,
            selectedChannel = selectedChannel,
            onTextChange = { newServerText ->
                lastServerText = newServerText
                radioTuneInModel.saveLastServerText(newServerText)
            },
            onTuneInClick = { channel ->
                selectedChannel = channel
                radioTuneInModel.startSnapclientService(lastServerText, channel)
            },
            onChannelChange = { channel ->
                selectedChannel = channel
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TuneInScreenContent(
    modifier: Modifier = Modifier,
    lastServerText: String,
    isButtonEnabled: Boolean,
    selectedChannel: AudioChannel,
    onTextChange: (String) -> Unit,
    onTuneInClick: (channel: AudioChannel) -> Unit,
    onChannelChange: (AudioChannel) -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = modifier.padding(innerPadding).fillMaxSize(),
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Tune In to another Radio:",
                            style = Typography.bodyMedium,
                        )
                    }

                    TextField(
                        value = lastServerText,
                        onValueChange = onTextChange,
                        textStyle = Typography.titleLarge,
                        placeholder = {
                            Text(
                                "Server IP",
                                style =
                                Typography.titleLarge,
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .border(
                                width = 1.dp,
                                color = Color.Black,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clip(RoundedCornerShape(12.dp)),
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            ButtonGroupDefaults.ConnectedSpaceBetween,
                        ),
                    ) {
                        AudioChannel.entries.forEach { channel ->
                            ToggleButton(
                                checked = selectedChannel == channel,
                                onCheckedChange = { onChannelChange(channel) },
                                modifier = Modifier.weight(channel.modifierWeight),
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
                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                Text(
                                    text = channel.label,
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { onTuneInClick(selectedChannel) },
                        enabled = isButtonEnabled,
                        modifier = Modifier
                            .padding(vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    ) {
                        Text("TUNE IN", style = Typography.titleLarge)
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
    name = "PreviewRadioTuneInContentDark",
    showSystemUi = true,
)
@Preview(
    showBackground = true,
    showSystemUi = true,
)
@Composable
fun PreviewRadioTuneInContent() {
    val lastServerText = "192.168.0.1"
    val isButtonEnabled = true
    val selectedChannel = AudioChannel.STEREO

    RadioTheme(schemeChoice = SchemeChoice.ORANGE) {
        TuneInScreenContent(
            lastServerText = lastServerText,
            isButtonEnabled = isButtonEnabled,
            selectedChannel = selectedChannel,
            onTextChange = {},
            onTuneInClick = {},
            onChannelChange = {},
        )
    }
}
