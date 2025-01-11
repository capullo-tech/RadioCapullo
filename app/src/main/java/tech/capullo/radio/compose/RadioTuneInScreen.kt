package tech.capullo.radio.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.capullo.radio.ui.theme.Typography
import tech.capullo.radio.ui.theme.primaryBlack
import tech.capullo.radio.ui.theme.secondaryOrange
import tech.capullo.radio.viewmodels.RadioTuneInModel

@Composable
fun RadioTuneInScreen(
    radioTuneInModel: RadioTuneInModel = hiltViewModel(),
    useDarkTheme: Boolean = false,
) {
    val colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray),
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
                    // Header Text
                    Text(
                        text = "Tune In to another Radio:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiary,
                    )
                    var lastServerText by remember {
                        mutableStateOf(radioTuneInModel.getLastServerText())
                    }
                    var isTunedIn by remember { mutableStateOf(false) }

                    LaunchedEffect(lastServerText) {
                    }

                    TextField(
                        value = lastServerText,
                        onValueChange = { newServerText ->
                            lastServerText = newServerText
                            radioTuneInModel.saveLastServerText(newServerText)
                        },
                        placeholder = {
                            Text(
                                "Server IP",
                                style = MaterialTheme.typography.titleLarge,
                                color = primaryBlack.copy(alpha = 0.7f),
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .border(
                                width = 1.dp,
                                color = primaryBlack.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clip(RoundedCornerShape(12.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = primaryBlack,
                            unfocusedTextColor = primaryBlack.copy(alpha = 0.7f),
                            focusedContainerColor = secondaryOrange.copy(alpha = 0.7f),
                            unfocusedContainerColor = secondaryOrange.copy(alpha = 0.7f),
                            cursorColor = primaryBlack,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )

//                    // DropdownMenu to display suggestions
                    /*                    DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            LazyColumn {
                                                items(suggestions.size) { index -> // Correctly iterate over the index of the list
                                                    val suggestion = suggestions[index] // Access the item at the index
                                                    DropdownMenuItem(
                                                        text = { Text(suggestion) },
                                                        onClick = {
                                                            lastServerText = suggestion
                                                            expanded = false
                                                    }
                                                    )
                                                }
                                            }
                                        } */
                    // Centered Button to Tune In
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .wrapContentSize(Alignment.Center), // Centers the Button
                    ) {
                        Button(
                            onClick = {
                                radioTuneInModel.initiateWorker(lastServerText)
                                isTunedIn = true
                            },
                            enabled = !isTunedIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .size(100.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                                .clip(CircleShape),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryBlack,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                            ),
                            shape = CircleShape,
                        ) {
                            Text("TUNE IN", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }
}
