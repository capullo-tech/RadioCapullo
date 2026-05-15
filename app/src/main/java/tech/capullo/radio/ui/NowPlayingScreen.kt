package tech.capullo.radio.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.json.JsonPrimitive
import tech.capullo.radio.snapcast.Client
import tech.capullo.radio.snapcast.Group
import tech.capullo.radio.snapcast.SnapcastControlClient
import tech.capullo.radio.snapcast.SnapclientProcess
import tech.capullo.radio.ui.model.AudioChannel
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.ui.theme.SchemeChoice
import tech.capullo.radio.viewmodels.NowPlayingUiState
import tech.capullo.radio.viewmodels.NowPlayingViewModel
import tech.capullo.radio.viewmodels.StreamCommand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(viewModel: NowPlayingViewModel = hiltViewModel()) {
    val uiState by viewModel.nowPlayingUiState.collectAsStateWithLifecycle()
    NowPlayingScreenContent(
        uiState = uiState,
        groups = viewModel.groups,
        onClientVolumeChange = viewModel::onClientVolumeChange,
        onClientLatencyChange = viewModel::onClientLatencyChange,
        onUpdateAudioChannel = viewModel::updateAudioChannel,
        onStreamControl = viewModel::onStreamControl,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreenContent(
    uiState: NowPlayingUiState,
    groups: List<Group>,
    onClientVolumeChange: (String, Boolean, Int) -> Unit,
    onClientLatencyChange: (String, Int) -> Unit,
    onUpdateAudioChannel: (AudioChannel) -> Unit,
    onStreamControl: (StreamCommand) -> Unit,
) {
    var showChannelDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    if (isExpanded) {
        BackHandler {
            isExpanded = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Radio Capullo",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Server: ${uiState.serverIp}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    actions = {
                        // Trimmed connection status
                        Surface(
                            shape = CircleShape,
                            color = when (uiState.snapclientProcessConnectionState) {
                                SnapclientProcess.ConnectionState.CONNECTED ->
                                    MaterialTheme.colorScheme.primaryContainer

                                SnapclientProcess.ConnectionState.ERROR ->
                                    MaterialTheme.colorScheme.errorContainer

                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }.copy(alpha = 0.7f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = when (
                                                uiState.snapclientProcessConnectionState
                                            ) {
                                                SnapclientProcess.ConnectionState.CONNECTED ->
                                                    MaterialTheme.colorScheme.primary

                                                SnapclientProcess.ConnectionState.ERROR ->
                                                    MaterialTheme.colorScheme.error

                                                else -> MaterialTheme.colorScheme.outline
                                            },
                                            shape = CircleShape,
                                        ),
                                )
                                Text(
                                    text = when (uiState.snapclientProcessConnectionState) {
                                        SnapclientProcess.ConnectionState.STARTING -> "Connecting"
                                        SnapclientProcess.ConnectionState.CONNECTED -> "Online"
                                        SnapclientProcess.ConnectionState.ERROR -> "Offline"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }

                        IconButton(onClick = { showChannelDialog = true }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                )
            },
            bottomBar = {
                if (uiState.snapclientProcessConnectionState ==
                    SnapclientProcess.ConnectionState.CONNECTED
                ) {
                    Surface(
                        onClick = {
                            android.util.Log.d(
                                "RadioCapullo",
                                "Toolbar clicked! Setting isExpanded to true",
                            )
                            isExpanded = true
                        },
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 8.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    android.util.Log.d(
                                        "RadioCapullo",
                                        "Row clicked! Setting isExpanded to true",
                                    )
                                    isExpanded = true
                                }
                                .navigationBarsPadding()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Album Art Placeholder
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable {
                                        android.util.Log.d(
                                            "RadioCapullo",
                                            "Album art clicked! Setting isExpanded to true",
                                        )
                                        isExpanded = true
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Album,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp),
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Metadata
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        android.util.Log.d(
                                            "RadioCapullo",
                                            "Metadata clicked! Setting isExpanded to true",
                                        )
                                        isExpanded = true
                                    },
                            ) {
                                Text(
                                    text = uiState.metadata?.title ?: "Radio Capullo",
                                    modifier = Modifier.basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        initialDelayMillis = 2000,
                                        repeatDelayMillis = 2000,
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                                Text(
                                    text = uiState.artistDisplay ?: "Unknown Artist",
                                    modifier = Modifier.basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        initialDelayMillis = 2000,
                                        repeatDelayMillis = 2000,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                )
                            }

                            // Controls
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(
                                    onClick = { onStreamControl(StreamCommand.Previous) },
                                    enabled = uiState.canGoPrevious,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipPrevious,
                                        contentDescription = "Previous",
                                    )
                                }
                                IconButton(
                                    onClick = { onStreamControl(StreamCommand.PlayPause) },
                                    enabled = if (uiState.playbackStatus ==
                                        "playing"
                                    ) {
                                        uiState.canPause
                                    } else {
                                        uiState.canPlay
                                    },
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(
                                        imageVector = if (uiState.playbackStatus == "playing") {
                                            Icons.Filled.Pause
                                        } else {
                                            Icons.Filled.PlayArrow
                                        },
                                        contentDescription = if (uiState.playbackStatus ==
                                            "playing"
                                        ) {
                                            "Pause"
                                        } else {
                                            "Play"
                                        },
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                                IconButton(
                                    onClick = { onStreamControl(StreamCommand.Next) },
                                    enabled = uiState.canGoNext,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipNext,
                                        contentDescription = "Next",
                                    )
                                }
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Show SnapserverGroups only when snapcastControlClient is connected
                if (uiState.snapcastControlClientConnectionState ==
                    SnapcastControlClient.ConnectionState.CONNECTED
                ) {
                    SnapserverGroups(
                        groups = groups,
                        onClientVolumeChange = onClientVolumeChange,
                        onClientLatencyChange = onClientLatencyChange,
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                                alpha = 0.3f,
                            ),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = when (uiState.snapcastControlClientConnectionState) {
                                    SnapcastControlClient.ConnectionState.STARTING ->
                                        "Connecting to Control..."

                                    SnapcastControlClient.ConnectionState.ERROR ->
                                        "Control Connection Error"

                                    SnapcastControlClient.ConnectionState.CONNECTED -> ""
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )

                            if (uiState.snapcastControlClientConnectionState ==
                                SnapcastControlClient.ConnectionState.ERROR
                            ) {
                                Text(
                                    text = "Check if Snapserver is running at ${uiState.serverIp}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }

            if (showChannelDialog) {
                AudioSettingsDialog(
                    onDismissRequest = { showChannelDialog = false },
                    selectedChannel = uiState.audioChannelState,
                    onCheckedChanged = { isChecked: Boolean, audioChannel: AudioChannel ->
                        if (isChecked && audioChannel != uiState.audioChannelState) {
                            onUpdateAudioChannel(audioChannel)
                        }
                    },
                )
            }
        }

        // Overlay Expanded Player
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            ) + fadeOut(animationSpec = tween(durationMillis = 300)),
        ) {
            ExpandedPlayerScreen(
                uiState = uiState,
                groups = groups,
                onClientVolumeChange = onClientVolumeChange,
                onStreamControl = onStreamControl,
                onCollapse = { isExpanded = false },
                onShowChannelDialog = { showChannelDialog = true },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedPlayerScreen(
    uiState: NowPlayingUiState,
    groups: List<Group>,
    onClientVolumeChange: (String, Boolean, Int) -> Unit,
    onStreamControl: (StreamCommand) -> Unit,
    onCollapse: () -> Unit,
    onShowChannelDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPlaying = uiState.playbackStatus == "playing"

    // Dynamic gradient background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        androidx.compose.ui.graphics.lerp(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primaryContainer,
                            0.25f,
                        ),
                        androidx.compose.ui.graphics.lerp(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.tertiaryContainer,
                            0.15f,
                        ),
                        MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp,
                )

                IconButton(onClick = onShowChannelDialog) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Audio Channel Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Expressive Album Art Card
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(
                        topStart = 64.dp,
                        topEnd = 24.dp,
                        bottomEnd = 64.dp,
                        bottomStart = 24.dp,
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        val containerColor =
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(containerColor),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Album,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(100.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Metadata Column
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = uiState.metadata?.title ?: "Radio Capullo",
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 2000,
                        repeatDelayMillis = 2000,
                    ),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = uiState.artistDisplay ?: "Unknown Artist",
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 2000,
                        repeatDelayMillis = 2000,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Expressive Visualizer (Waveform)
            WaveformVisualizer(
                isPlaying = isPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(
                    onClick = { onStreamControl(StreamCommand.Previous) },
                    enabled = uiState.canGoPrevious,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(36.dp),
                        tint = if (uiState.canGoPrevious) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                    )
                }

                FilledIconButton(
                    onClick = { onStreamControl(StreamCommand.PlayPause) },
                    enabled = if (isPlaying) uiState.canPause else uiState.canPlay,
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = if (isPlaying) {
                            Icons.Filled.Pause
                        } else {
                            Icons.Filled.PlayArrow
                        },
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(40.dp),
                    )
                }

                IconButton(
                    onClick = { onStreamControl(StreamCommand.Next) },
                    enabled = uiState.canGoNext,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(36.dp),
                        tint = if (uiState.canGoNext) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun WaveformVisualizer(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "h1",
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "h2",
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "h3",
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "h4",
    )
    val h5 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "h5",
    )

    val color = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val barCount = 19
        val barWidth = 8.dp.toPx()
        val spacing = 6.dp.toPx()
        val totalWidth = barCount * barWidth + (barCount - 1) * spacing
        val startX = (size.width - totalWidth) / 2f
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val factor = if (isPlaying) {
                when (i % 5) {
                    0 -> h1
                    1 -> h2
                    2 -> h3
                    3 -> h4
                    else -> h5
                }
            } else {
                0.08f
            }

            val barHeight = size.height * factor
            val x = startX + i * (barWidth + spacing)
            val y = centerY - barHeight / 2f

            drawRoundRect(
                color = color.copy(alpha = 0.8f - (i % 3) * 0.1f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NowPlayingScreenContentPreview() {
    val uiState = NowPlayingUiState(
        audioChannelState = AudioChannel.STEREO,
        serverIp = "192.168.1.100",
        snapclientProcessConnectionState = SnapclientProcess.ConnectionState.CONNECTED,
        snapcastControlClientConnectionState = SnapcastControlClient.ConnectionState.CONNECTED,
        playbackStatus = "playing",
        metadata = tech.capullo.radio.snapcast.StreamMetadata(
            title = "Bohemian Rhapsody",
            artist = JsonPrimitive("Queen"),
            album = "A Night at the Opera",
        ),
        canPlay = true,
        canPause = true,
        canGoNext = true,
        canGoPrevious = true,
        artistDisplay = "Queen",
    )
    RadioTheme(schemeChoice = SchemeChoice.ORANGE) {
        NowPlayingScreenContent(
            uiState = uiState,
            groups = mockSnapcastGroups,
            onClientVolumeChange = { _, _, _ -> },
            onClientLatencyChange = { _, _ -> },
            onUpdateAudioChannel = {},
            onStreamControl = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NowPlayingScreenContentErrorPreview() {
    val uiState = NowPlayingUiState(
        audioChannelState = AudioChannel.STEREO,
        serverIp = "192.168.1.100",
        snapclientProcessConnectionState = SnapclientProcess.ConnectionState.CONNECTED,
        snapcastControlClientConnectionState = SnapcastControlClient.ConnectionState.ERROR,
        canPlay = false,
        canPause = false,
        canGoNext = false,
        canGoPrevious = false,
    )
    RadioTheme(schemeChoice = SchemeChoice.ORANGE) {
        NowPlayingScreenContent(
            uiState = uiState,
            groups = emptyList(), // No groups when control client is not connected
            onClientVolumeChange = { _, _, _ -> },
            onClientLatencyChange = { _, _ -> },
            onUpdateAudioChannel = {},
            onStreamControl = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ExpandedPlayerScreenPreview() {
    val uiState = NowPlayingUiState(
        audioChannelState = AudioChannel.STEREO,
        serverIp = "192.168.1.100",
        snapclientProcessConnectionState = SnapclientProcess.ConnectionState.CONNECTED,
        snapcastControlClientConnectionState = SnapcastControlClient.ConnectionState.CONNECTED,
        playbackStatus = "playing",
        metadata = tech.capullo.radio.snapcast.StreamMetadata(
            title = "Bohemian Rhapsody",
            artist = JsonPrimitive("Queen"),
            album = "A Night at the Opera",
        ),
        canPlay = true,
        canPause = true,
        canGoNext = true,
        canGoPrevious = true,
        artistDisplay = "Queen",
    )
    RadioTheme(schemeChoice = SchemeChoice.ORANGE) {
        ExpandedPlayerScreen(
            uiState = uiState,
            groups = mockSnapcastGroups,
            onClientVolumeChange = { _, _, _ -> },
            onStreamControl = {},
            onCollapse = {},
            onShowChannelDialog = {},
        )
    }
}
