package tech.capullo.radio.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.capullo.radio.snapcast.DiscoveredSnapserver
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
    var selectedServer by remember { mutableStateOf<DiscoveredSnapserver?>(null) }
    var isTunedIn by remember { mutableStateOf(false) }

    // Collect connection state from ViewModel
    val connectionState by radioTuneInModel.connectionState.collectAsState()

    // Collect discovered services from ViewModel
    val discoveredServices by radioTuneInModel.discoveredServices.collectAsState()

    // Collect discovery state from ViewModel
    val isDiscovering by radioTuneInModel.isDiscovering.collectAsState()

    // Navigate when service is connected and running
    if (connectionState.isConnected &&
        connectionState.serverIp.isNotEmpty()
    ) {
        onConnected()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { radioTuneInModel.startDiscovery() },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        },
    ) { innerPadding ->
        TuneInScreenContent(
            modifier = Modifier.padding(innerPadding),
            discoveredServices = discoveredServices,
            isDiscovering = isDiscovering,
            selectedServer = selectedServer,
            isTunedIn = isTunedIn,
            manualIpText = lastServerText,
            selectedChannel = selectedChannel,
            onManualIpTextChange = {
                lastServerText = it
                radioTuneInModel.saveLastServerText(it)
            },
            onServerSelected = { server ->
                selectedServer = server
                lastServerText = server.hostAddress
                radioTuneInModel.saveLastServerText(server.hostAddress)
            },
            onTuneInClick = { channel ->
                selectedChannel = channel
                selectedServer?.let { server ->
                    radioTuneInModel.connectToDiscoveredService(server, channel)
                    isTunedIn = true
                } ?: run {
                    if (lastServerText.isNotEmpty()) {
                        radioTuneInModel.startSnapclientService(lastServerText, channel)
                        isTunedIn = true
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TuneInScreenContent(
    modifier: Modifier = Modifier,
    discoveredServices: List<DiscoveredSnapserver>,
    isDiscovering: Boolean,
    selectedServer: DiscoveredSnapserver?,
    isTunedIn: Boolean,
    manualIpText: String,
    selectedChannel: AudioChannel,
    onManualIpTextChange: (String) -> Unit,
    onServerSelected: (DiscoveredSnapserver) -> Unit,
    onTuneInClick: (channel: AudioChannel) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Discover Snapcast Servers",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isDiscovering) {
                        "Scanning for servers..."
                    } else {
                        "${discoveredServices.size} servers found"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }

        // Server list
        if (discoveredServices.isEmpty()) {
            EmptyServerList(isDiscovering)
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                items(discoveredServices) { server ->
                    ServerListItem(
                        server = server,
                        isSelected = selectedServer == server,
                        onClick = { onServerSelected(server) },
                    )
                }
            }
        }

        // Manual IP input
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                TextField(
                    value = manualIpText,
                    onValueChange = onManualIpTextChange,
                    textStyle = Typography.bodyLarge,
                    placeholder = { Text("Enter server IP address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
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
                            onCheckedChange = { onTuneInClick(channel) },
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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Selected Channel: ${selectedChannel.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                val tuneInEnabled =
                    !isTunedIn &&
                        (selectedServer != null || manualIpText.isNotEmpty())

                Button(
                    onClick = { onTuneInClick(selectedChannel) },
                    enabled = tuneInEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Text("TUNE IN", style = Typography.titleLarge)
                }
            }
        }


    }
}

@Composable
fun ServerListItem(server: DiscoveredSnapserver, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Signal icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        },
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Server",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Server info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = server.serviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = server.hostAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )

                Text(
                    text = "Port: ${server.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }

            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyServerList(isDiscovering: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isDiscovering) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Searching for Snapcast servers...",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "No servers found",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Snapcast servers found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Make sure Snapcast servers are running on your network",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
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
    val discoveredServices = listOf(
        DiscoveredSnapserver("Snapcast Server", "_snapcast._tcp", "192.168.1.100", 1704),
        DiscoveredSnapserver("My Snapcast", "_snapcast._tcp", "192.168.1.101", 1704),
    )

    RadioTheme(schemeChoice = SchemeChoice.ORANGE) {
        TuneInScreenContent(
            discoveredServices = discoveredServices,
            isDiscovering = false,
            selectedServer = discoveredServices.first(),
            isTunedIn = false,
            manualIpText = "192.168.0.1",
            selectedChannel = AudioChannel.STEREO,
            onManualIpTextChange = {},
            onServerSelected = {},
            onTuneInClick = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "PreviewEmptyServerList",
)
@Composable
fun PreviewEmptyServerList() {
    RadioTheme(schemeChoice = SchemeChoice.GREEN) {
        EmptyServerList(
            isDiscovering = false,
        )
    }
}

@Preview(
    showBackground = true,
    name = "PreviewServerListItem",
)
@Composable
fun PreviewServerListItem() {
    RadioTheme(schemeChoice = SchemeChoice.GREEN) {
        ServerListItem(
            server = DiscoveredSnapserver(
                "Living Room Snapcast",
                "_snapcast._tcp",
                "192.168.0.10",
                1704,
            ),
            isSelected = true,
            onClick = {},
        )
    }
}
