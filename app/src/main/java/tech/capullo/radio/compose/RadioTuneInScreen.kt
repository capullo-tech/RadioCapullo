package tech.capullo.radio.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import tech.capullo.radio.snapcast.SnapclientProcess.BalanceState
import tech.capullo.radio.ui.theme.Typography
import tech.capullo.radio.ui.theme.onPrimaryLight
import tech.capullo.radio.ui.theme.onSecondaryLight
import tech.capullo.radio.ui.theme.secondaryOrange
import tech.capullo.radio.ui.theme.surfaceLight
import tech.capullo.radio.viewmodels.RadioTuneInModel
import tech.capullo.radio.viewmodels.SnapcastViewModel

@Composable
fun RadioTuneInScreen(
    radioTuneInModel: RadioTuneInModel = hiltViewModel(),
    snapcastViewModel: SnapcastViewModel = hiltViewModel(),
    useDarkTheme: Boolean = false,
) {
    var lastServerText by remember {
        mutableStateOf(radioTuneInModel.getLastServerText())
    }
    var isTunedIn by remember { mutableStateOf(false) }
    val balanceState by snapcastViewModel.balanceState.collectAsState()

    val colorScheme = if (useDarkTheme) MaterialTheme.colorScheme else MaterialTheme.colorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        RadioTuneInContent(
            lastServerText = lastServerText,
            isTunedIn = isTunedIn,
            balanceState = balanceState,
            onTextChange = { newServerText ->
                lastServerText = newServerText
                radioTuneInModel.saveLastServerText(newServerText)
            },
            onTuneInClick = {
                radioTuneInModel.startSnapclientService(lastServerText)
                isTunedIn = true
            },
            onBalanceChange = { state ->
                snapcastViewModel.setBalanceState(state)
            },
        )
    }
}

@Composable
fun RadioTuneInContent(
    lastServerText: String,
    isTunedIn: Boolean,
    balanceState: BalanceState,
    onTextChange: (String) -> Unit,
    onTuneInClick: () -> Unit,
    onBalanceChange: (BalanceState) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceLight)
            .imePadding(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.5f),
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

                Text(
                    text = "Audio Balance Control:",
                    style = Typography.bodyMedium,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RadioButton(
                            selected = balanceState == BalanceState.LEFT_ONLY,
                            onClick = { onBalanceChange(BalanceState.LEFT_ONLY) }
                        )
                        Text(
                            "Left Only",
                            style = Typography.bodySmall,
                            color = Color.Black
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RadioButton(
                            selected = balanceState == BalanceState.RIGHT_ONLY,
                            onClick = { onBalanceChange(BalanceState.RIGHT_ONLY) }
                        )
                        Text(
                            "Right Only",
                            style = Typography.bodySmall,
                            color = Color.Black
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RadioButton(
                            selected = balanceState == BalanceState.BALANCED,
                            onClick = { onBalanceChange(BalanceState.BALANCED) }
                        )
                        Text(
                            "Balanced",
                            style = Typography.bodySmall,
                            color = Color.Black
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .wrapContentSize(Alignment.Center),
                ) {
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
}

@Preview(showBackground = true)
@Composable
fun PreviewRadioTuneInContent() {
    val lastServerText = "192.168.0.1"
    val isTunedIn = false
    val balanceState = BalanceState.BALANCED

    RadioTuneInContent(
        lastServerText = lastServerText,
        isTunedIn = isTunedIn,
        balanceState = balanceState,
        onTextChange = {},
        onTuneInClick = {},
        onBalanceChange = {},
    )
}