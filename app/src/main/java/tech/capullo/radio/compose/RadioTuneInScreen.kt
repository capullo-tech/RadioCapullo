package tech.capullo.radio.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
            RadioTuneInContent(
                modifier = Modifier
                    .padding(innerPadding),
                lastServerText = lastServerText,
                isTunedIn = isTunedIn,
                onTextChange = { newServerText ->
                    lastServerText = newServerText
                    radioTuneInModel.saveLastServerText(newServerText)
                },
                onTuneInClick = {
                    radioTuneInModel.startSnapclientService(lastServerText)
                    isTunedIn = true
                },
            )
        }
    }
}

val options = listOf("Left", "Stereo", "Right")
val unCheckedIcons =
    listOf(Icons.Outlined.MoreVert, Icons.Outlined.Notifications, Icons.Outlined.PlayArrow)
val checkedIcons = listOf(Icons.Filled.MoreVert, Icons.Filled.Notifications, Icons.Filled.PlayArrow)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RadioTuneInContent(
    modifier: Modifier = Modifier,
    lastServerText: String,
    isTunedIn: Boolean,
    onTextChange: (String) -> Unit,
    onTuneInClick: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = modifier.padding(innerPadding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            //verticalArrangement = Arrangement.Center,
        ) {
            // =====================================
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                //.fillMaxHeight(0.9f),
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

                    var selectedIndex by remember { mutableIntStateOf(0) }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    ) {
                        val modifiers =
                            listOf(Modifier.weight(1f), Modifier.weight(1.5f), Modifier.weight(1f))
                        options.forEachIndexed { index, label ->
                            ToggleButton(
                                checked = selectedIndex == index,
                                onCheckedChange = { selectedIndex = index },
                                modifier = modifiers[index],
                                shapes =
                                    when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    if (selectedIndex == index) checkedIcons[index] else unCheckedIcons[index],
                                    contentDescription = "Localized description",
                                )
                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                Text(
                                    text = label,
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onTuneInClick,
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
    /*
    // Add LaunchedEffect to listen for changes in lastServerText
    LaunchedEffect(lastServerText) {
        // Example: radioTuneInModel.saveLastServerText(lastServerText)
    }

     */
}

@Preview(showBackground = true)
@Composable
fun PreviewRadioTuneInContent() {
    val lastServerText = "192.168.0.1"
    val isTunedIn = false

    RadioTuneInContent(
        lastServerText = lastServerText,
        isTunedIn = isTunedIn,
        onTextChange = {},
        onTuneInClick = {},
    )
}
