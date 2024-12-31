package tech.capullo.radio.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tech.capullo.radio.ui.theme.*
import tech.capullo.radio.viewmodels.RadioTuneInModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect

@Composable
fun RadioTuneInScreen(
    radioTuneInModel: RadioTuneInModel = hiltViewModel(),
    useDarkTheme: Boolean = false // Option to toggle between light and dark themes
) {
    // Choose the color scheme based on the theme preference
    val colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray),
            contentAlignment = Alignment.BottomEnd
        ) {
            // Main Card containing everything
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = tertiaryOrange) // Custom background color for the card
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Text
                    Text(
                        text = "Tune In to another Radio:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                    var lastServerText by remember {
                        mutableStateOf(radioTuneInModel.getLastServerText())
                    }
                    var isTunedIn by remember { mutableStateOf(false) }

                    // Filter suggestions based on the input text


                    LaunchedEffect(lastServerText) {
                    }

                    // TextField for user input
                    TextField(
                        value = lastServerText,
                        onValueChange = { newServerText ->
                            lastServerText = newServerText
                            radioTuneInModel.saveLastServerText(newServerText) // Save the new server text
                        },
                        placeholder = {
                            Text(
                                "Server IP",
                                style = MaterialTheme.typography.titleLarge,
                                color = secondaryBlack.copy(alpha = 0.7f) // Dimmed placeholder color
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp) // Ensures TextField takes full width
                            .border(
                                width = 1.dp,
                                color = secondaryBlack.copy(alpha = 0.3f), // Thin border with light color
                                shape = RoundedCornerShape(12.dp) // Rounded corners with 12dp radius
                            )
                            .clip(RoundedCornerShape(12.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = secondaryBlack,
                            unfocusedTextColor = secondaryBlack.copy(alpha = 0.7f),
                            focusedContainerColor = tertiaryOrange.copy(alpha = 0.7f),
                            unfocusedContainerColor = tertiaryOrange.copy(alpha = 0.7f),
                            cursorColor = secondaryBlack,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
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
                            .wrapContentSize(Alignment.Center) // Centers the Button
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
                                containerColor = secondaryBlack, // Using the predefined orange
                                contentColor = MaterialTheme.colorScheme.onSecondary // Ensures white text on orange button
                            ),
                            shape = CircleShape
                        ) {
                            Text("TUNE IN", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }
}
