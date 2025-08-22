package tech.capullo.radio.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
//import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.capullo.radio.snapcast.SnapcastServer
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.ui.theme.SchemeChoice
import tech.capullo.radio.ui.theme.Typography
import tech.capullo.radio.viewmodels.RadioTuneInModel

@Composable
fun RadioTuneInScreen(radioTuneInModel: RadioTuneInModel = hiltViewModel()) {
    val discoveredServers by radioTuneInModel.discoveredServers.collectAsState()
    val isDiscovering by radioTuneInModel.isDiscovering.collectAsState()
    val selectedServer by radioTuneInModel.selectedServer.collectAsState()
    var isTunedIn by remember { mutableStateOf(false) }
    
    // For manual IP entry (fallback)
    var manualIpVisible by remember { mutableStateOf(false) }
    var manualIpText by remember { mutableStateOf(radioTuneInModel.getLastServerText()) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { radioTuneInModel.startDiscovery() },
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    ) { innerPadding ->
        RadioTuneInScreenContent(
            modifier = Modifier.padding(innerPadding),
            discoveredServers = discoveredServers,
            isDiscovering = isDiscovering,
            selectedServer = selectedServer,
            isTunedIn = isTunedIn,
            manualIpVisible = manualIpVisible,
            manualIpText = manualIpText,
            onManualIpVisibilityChange = { manualIpVisible = it },
            onManualIpTextChange = { 
                manualIpText = it 
                radioTuneInModel.saveLastServerText(it)
            },
            onServerSelected = { server ->
                radioTuneInModel.selectServer(server)
            },
            onTuneInClick = { channel ->
                selectedServer?.let { server ->
                    radioTuneInModel.startSnapclientService(server, channel)
                    isTunedIn = true
                } ?: run {
                    if (manualIpVisible && manualIpText.isNotEmpty()) {
                        radioTuneInModel.startSnapclientService(manualIpText, channel)
                        isTunedIn = true
                    }
                }
            },
        )
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
        "Stereo",
    ),
    RIGHT(
        selectedIcon = Icons.Filled.PlayArrow,
        unselectedIcon = Icons.Outlined.PlayArrow,
        modifierWeight = 1f,
        "Right",
    ),
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RadioTuneInScreenContent(
    modifier: Modifier = Modifier,
    discoveredServers: List<SnapcastServer>,
    isDiscovering: Boolean,
    selectedServer: SnapcastServer?,
    isTunedIn: Boolean,
    manualIpVisible: Boolean,
    manualIpText: String,
    onManualIpVisibilityChange: (Boolean) -> Unit,
    onManualIpTextChange: (String) -> Unit,
    onServerSelected: (SnapcastServer) -> Unit,
    onTuneInClick: (channel: AudioChannel) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Discover Snapcast Servers",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isDiscovering) "Scanning for servers..." else "${discoveredServers.size} servers found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        
        // Server list
        if (discoveredServers.isEmpty()) {
            EmptyServerList(isDiscovering, onManualIpVisibilityChange)
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(discoveredServers) { server ->
                    ServerListItem(
                        server = server,
                        isSelected = selectedServer == server,
                        onClick = { onServerSelected(server) }
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { onManualIpVisibilityChange(!manualIpVisible) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Manual IP",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enter IP manually",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        // Manual IP input
        AnimatedVisibility(
            visible = manualIpVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
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
                    Text(
                        text = "Manual IP Address",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextField(
                        value = manualIpText,
                        onValueChange = onManualIpTextChange,
                        textStyle = Typography.bodyLarge,
                        placeholder = { Text("Server IP") },
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
                }
            }
        }
        
        // Audio channel selection and tune-in button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                var selectedChannel by rememberSaveable { mutableStateOf(AudioChannel.STEREO) }
                
                Text(
                    text = "Audio Channel",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        ButtonGroupDefaults.ConnectedSpaceBetween,
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AudioChannel.entries.forEach { channel ->
                        ToggleButton(
                            checked = selectedChannel == channel,
                            onCheckedChange = { selectedChannel = channel },
                            modifier = Modifier.weight(channel.modifierWeight),
                            shapes = when (channel) {
                                AudioChannel.LEFT -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                AudioChannel.RIGHT -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                AudioChannel.STEREO -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Icon(
                                if (selectedChannel == channel) channel.selectedIcon else channel.unselectedIcon,
                                contentDescription = channel.label,
                            )
                            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                            Text(text = channel.label)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                val tuneInEnabled = !isTunedIn && (selectedServer != null || (manualIpVisible && manualIpText.isNotEmpty()))
                
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
fun ServerListItem(
    server: SnapcastServer,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Signal icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Server",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Server info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = server.serviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = server.host,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                
                Text(
                    text = "Port: ${server.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            
            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyServerList(
    isDiscovering: Boolean,
    onManualIpVisibilityChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
            //.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isDiscovering) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Searching for Snapcast servers...",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        } else {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "No servers found",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No Snapcast servers found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Make sure Snapcast servers are running on your network",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { onManualIpVisibilityChange(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Enter IP Manually")
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
    val sampleServers = listOf(
        SnapcastServer("Living Room Snapcast", "192.168.0.10", 1704),
        SnapcastServer("Kitchen Audio", "192.168.0.15", 1704),
        SnapcastServer("Bedroom Speaker", "192.168.0.20", 1704)
    )

    RadioTheme(schemeChoice = SchemeChoice.ORANGE) {
        RadioTuneInScreenContent(
            discoveredServers = sampleServers,
            isDiscovering = false,
            selectedServer = sampleServers.first(),
            isTunedIn = false,
            manualIpVisible = false,
            manualIpText = "192.168.0.1",
            onManualIpVisibilityChange = {},
            onManualIpTextChange = {},
            onServerSelected = {},
            onTuneInClick = {}
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
            onManualIpVisibilityChange = {}
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
            server = SnapcastServer("Living Room Snapcast", "192.168.0.10", 1704),
            isSelected = true,
            onClick = {}
        )
    }
}
