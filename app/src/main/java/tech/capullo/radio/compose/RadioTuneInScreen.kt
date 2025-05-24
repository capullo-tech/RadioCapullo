package tech.capullo.radio.compose

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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
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
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.capullo.radio.ui.theme.Typography
import tech.capullo.radio.ui.theme.onPrimaryLight
import tech.capullo.radio.ui.theme.onSecondaryLight
import tech.capullo.radio.ui.theme.secondaryOrange
import tech.capullo.radio.ui.theme.surfaceLight
import tech.capullo.radio.viewmodels.RadioTuneInModel

@Composable
fun RadioTuneInScreen(
    radioTuneInModel: RadioTuneInModel = hiltViewModel(),
    useDarkTheme: Boolean = false,
) {
    var lastServerText by remember {
        mutableStateOf(radioTuneInModel.getLastServerText())
    }
    var isTunedIn by remember { mutableStateOf(false) }

    val colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        Scaffold { innerPadding ->
            RadioTuneInScreenContent(
                modifier = Modifier
                    .padding(innerPadding),
                lastServerText = lastServerText,
                isTunedIn = isTunedIn,
                onTextChange = { newServerText ->
                    lastServerText = newServerText
                    radioTuneInModel.saveLastServerText(newServerText)
                },
                onTuneInClick = { channel ->
                    radioTuneInModel.startSnapclientService(lastServerText, channel)
                    isTunedIn = true
                },
            )
        }
    }
}

enum class AudioChannel(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val modifierWeight: Float,
    val label: String,
    ) {
    LEFT(
        selectedIcon = Icons.Filled.MoreVert,
        unselectedIcon = Icons.Outlined.MoreVert,
        modifierWeight = 1f,
        "Left",
    ),
    STEREO(
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications,
        modifierWeight = 1.5f,
        "Stereo"
    ),
    RIGHT(
        selectedIcon = Icons.Filled.PlayArrow,
        unselectedIcon = Icons.Outlined.PlayArrow,
        modifierWeight = 1f,
        "Right"
    ),
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RadioTuneInScreenContent(
    modifier: Modifier = Modifier,
    lastServerText: String,
    isTunedIn: Boolean,
    onTextChange: (String) -> Unit,
    onTuneInClick: (channel: AudioChannel) -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = modifier.padding(innerPadding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = secondaryOrange),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tune In to another Radio:",
                        style = Typography.bodyMedium,
                        color = Color.Black,
                    )

                    TextField(
                        value = lastServerText,
                        onValueChange = onTextChange,
                        textStyle = Typography.titleLarge.copy(color = onSecondaryLight),
                        placeholder = {
                            Text(
                                "Server IP",
                                style =
                                    Typography
                                        .titleLarge.copy(color = onSecondaryLight.copy(alpha = 0.7f)),
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
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = onSecondaryLight,
                            unfocusedTextColor = onSecondaryLight,
                            focusedContainerColor = surfaceLight.copy(alpha = 0.2f),
                            unfocusedContainerColor = secondaryOrange,
                            cursorColor = Color.Black,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )

                    var selectedChannel by rememberSaveable { mutableStateOf(AudioChannel.STEREO) }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    ) {
                        AudioChannel.entries.forEach { channel ->
                            ToggleButton(
                                checked = selectedChannel == channel,
                                onCheckedChange = { selectedChannel = channel },
                                modifier = Modifier.weight(channel.modifierWeight),
                                shapes =
                                    when (channel) {
                                        AudioChannel.LEFT -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        AudioChannel.RIGHT -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        AudioChannel.STEREO -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                                contentPadding = PaddingValues(0.dp)
                            ) {

                                Icon(
                                    if (selectedChannel == channel) channel.selectedIcon else channel.unselectedIcon,
                                    contentDescription = channel.label,
                                )
                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                Text(
                                    text = channel.label
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { onTuneInClick(selectedChannel) },
                        enabled = !isTunedIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .size(100.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .clip(CircleShape),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = onPrimaryLight,
                        ),
                        shape = CircleShape,
                    ) {
                        Text("TUNE IN", style = Typography.titleLarge)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRadioTuneInContent() {
    val lastServerText = "192.168.0.1"
    val isTunedIn = false

    RadioTuneInScreenContent(
        lastServerText = lastServerText,
        isTunedIn = isTunedIn,
        onTextChange = {},
        onTuneInClick = {},
    )
}
