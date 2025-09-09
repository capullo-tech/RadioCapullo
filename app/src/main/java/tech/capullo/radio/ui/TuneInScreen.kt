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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import tech.capullo.radio.snapcast.DiscoveredSnapserver
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.ui.theme.SchemeChoice
import tech.capullo.radio.ui.theme.Typography
import tech.capullo.radio.viewmodels.TuneInModel
import tech.capullo.radio.viewmodels.TuneInState

@Composable
fun TuneInScreen(viewModel: TuneInModel = hiltViewModel(), onConnected: () -> Unit) {
    val uiState by viewModel.tuneInState.collectAsState()

    // Navigate when service is connected and running
    if (uiState.isTunedIn) {
        onConnected()
    }

    Scaffold { innerPadding ->
        TuneInScreenContent(
            modifier = Modifier.padding(innerPadding),
            uiState = uiState,
            onServerIPTextFieldValueChanged = viewModel::onServerIPTextFieldValueChanged,
            onTuneInClick = viewModel::startSnapclientService,
            onServerSelected = { server: DiscoveredSnapserver ->
                viewModel.onServerIPTextFieldValueChanged(server.hostAddress)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TuneInScreenContent(
    modifier: Modifier = Modifier,
    uiState: TuneInState,
    onServerIPTextFieldValueChanged: (String) -> Unit,
    onTuneInClick: () -> Unit,
    onServerSelected: (DiscoveredSnapserver) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
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
                TextField(
                    value = uiState.serverIp,
                    onValueChange = onServerIPTextFieldValueChanged,
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

                Button(
                    onClick = { onTuneInClick() },
                    enabled = uiState.serverIp.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("TUNE IN", style = Typography.titleLarge)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Scanning for servers... Found ${uiState.availableServers.size} servers",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uiState.availableServers.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                items(uiState.availableServers) { server ->
                    ServerListItem(
                        server = server,
                        isSelected = uiState.serverIp == server.hostAddress,
                        onClick = { onServerSelected(server) },
                    )
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
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = server.hostAddress,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.weight(1f))

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
        DiscoveredSnapserver("Snapcast Server 1", "_snapcast._tcp", "192.168.1.100", 1704),
        DiscoveredSnapserver("Snapcast Server 2", "_snapcast._tcp", "192.168.1.101", 1704),
        DiscoveredSnapserver("Snapcast Server 3", "_snapcast._tcp", "192.168.1.102", 1704),
        DiscoveredSnapserver("Snapcast Server 4", "_snapcast._tcp", "192.168.1.103", 1704),
        DiscoveredSnapserver("Snapcast Server 5", "_snapcast._tcp", "192.168.1.104", 1704),
        DiscoveredSnapserver("Snapcast Server 6", "_snapcast._tcp", "192.168.1.105", 1704),
        DiscoveredSnapserver("Snapcast Server 7", "_snapcast._tcp", "192.168.1.106", 1704),
        DiscoveredSnapserver("Snapcast Server 8", "_snapcast._tcp", "192.168.1.107", 1704),
        DiscoveredSnapserver("Snapcast Server 9", "_snapcast._tcp", "192.168.1.108", 1704),
        DiscoveredSnapserver("Snapcast Server 10", "_snapcast._tcp", "192.168.1.109", 1704),
    )

    val uiState = TuneInState(
        availableServers = discoveredServices,
    )
    RadioTheme(schemeChoice = SchemeChoice.ORANGE) {
        Scaffold { innerPadding ->
            TuneInScreenContent(
                modifier = Modifier.padding(innerPadding),
                uiState = uiState,
                onServerIPTextFieldValueChanged = {},
                onTuneInClick = {},
                onServerSelected = {},
            )
        }
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
