package tech.capullo.radio.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.capullo.radio.snapcast.Client
import tech.capullo.radio.snapcast.ClientConfig
import tech.capullo.radio.snapcast.Group
import tech.capullo.radio.snapcast.Host
import tech.capullo.radio.snapcast.LastSeen
import tech.capullo.radio.snapcast.SnapClient
import tech.capullo.radio.snapcast.Volume
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.ui.theme.SchemeChoice
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

private enum class KnobMode { LATENCY, VOLUME }

@Composable
fun SnapserverGroups(
    modifier: Modifier = Modifier,
    groups: List<Group>,
    onClientVolumeChange: (String, Boolean, Int) -> Unit,
    onClientLatencyChange: (String, Int) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        groups.forEach { group ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        text = group.name.ifEmpty { group.streamId.ifEmpty { group.id } },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                }
            }

            items(
                items = group.clients.filter { it.connected },
                key = { client -> client.id },
            ) { client ->
                SnapcastClientCard(
                    name = client.host.name,
                    muted = client.config.volume.muted,
                    onMutedChange = { muted ->
                        onClientVolumeChange(
                            client.id,
                            muted,
                            client.config.volume.percent,
                        )
                    },
                    volume = client.config.volume.percent.toFloat() / 100f,
                    onVolumeChange = { volume ->
                        onClientVolumeChange(
                            client.id,
                            client.config.volume.muted,
                            volume,
                        )
                    },
                    latency = client.config.latency,
                    onLatencyChange = { latency ->
                        onClientLatencyChange(client.id, latency)
                    },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SnapcastClientCard(
    name: String,
    muted: Boolean,
    onMutedChange: (Boolean) -> Unit,
    volume: Float,
    onVolumeChange: (Int) -> Unit,
    latency: Int,
    onLatencyChange: (Int) -> Unit,
) {
    // Context: When sending ClientSetVolume/Latency requests, we DO NOT process the response;
    // state is managed locally for UI responsiveness. Server notifications sync via LaunchedEffect.
    var mutedState by remember { mutableStateOf(muted) }
    var volumeState by remember { mutableFloatStateOf(volume) }
    var latencyState by remember { mutableStateOf(latency) }
    var knobMode by remember { mutableStateOf(KnobMode.LATENCY) }

    LaunchedEffect(muted) { mutedState = muted }
    LaunchedEffect(volume) { volumeState = volume }
    LaunchedEffect(latency) { latencyState = latency }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        shape = RoundedCornerShape(
            topStart = 32.dp,
            topEnd = 32.dp,
            bottomEnd = 32.dp,
            bottomStart = 8.dp,
        ),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val knobBaseSize = maxWidth * 0.75f
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 14.dp, end = 16.dp),
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ClientKnob(
                        mode = knobMode,
                        onModeToggle = {
                            knobMode =
                                if (knobMode ==
                                    KnobMode.LATENCY
                                ) {
                                    KnobMode.VOLUME
                                } else {
                                    KnobMode.LATENCY
                                }
                        },
                        latency = latencyState,
                        onLatencyChange = { newLatency ->
                            latencyState = newLatency
                            onLatencyChange(newLatency)
                        },
                        volume = round(volumeState * 100f).toInt(),
                        onVolumeChange = { newVolume ->
                            volumeState = newVolume / 100f
                            onVolumeChange(newVolume)
                        },
                        muted = mutedState,
                        onMutedToggle = {
                            mutedState = !mutedState
                            onMutedChange(mutedState)
                        },
                        baseSize = knobBaseSize,
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientKnob(
    mode: KnobMode,
    onModeToggle: () -> Unit,
    latency: Int,
    onLatencyChange: (Int) -> Unit,
    volume: Int,
    onVolumeChange: (Int) -> Unit,
    muted: Boolean,
    onMutedToggle: () -> Unit,
    baseSize: Dp = 124.dp,
) {
    val minLatency = -500
    val maxLatency = 1000
    // Tuned for higher responsiveness so users need less circular travel per ms/% step.
    val degreesPerStep = 7f
    val minActiveRadiusRatio = 0.28f
    val tinyMotionDeadzoneDeg = 0.18f
    val maxEventDeltaDeg = 10f
    val smoothingPrevWeight = 0.35f
    val smoothingCurrentWeight = 0.65f
    val angularGain = 1.35f
    var isActive by remember { mutableStateOf(false) }
    var displayLatency by remember { mutableStateOf(latency) }
    var displayVolume by remember { mutableIntStateOf(volume) }
    var center by remember { mutableStateOf(Offset.Zero) }
    var knobRadiusPx by remember { mutableStateOf(0f) }
    var previousSmoothedDeltaDeg by remember { mutableStateOf(0f) }
    var accumulatedDelta by remember { mutableStateOf(0f) }

    val knobSize by animateDpAsState(
        targetValue = if (isActive) baseSize * 1.28f else baseSize,
        label = "knobSize",
    )
    val indicatorScale by animateFloatAsState(
        targetValue = if (isActive) 1.06f else 1f,
        label = "knobScale",
    )

    LaunchedEffect(latency) { displayLatency = latency }
    LaunchedEffect(volume) { displayVolume = volume }

    val knobFillColor = when {
        isActive && mode == KnobMode.LATENCY -> MaterialTheme.colorScheme.primaryContainer
        isActive && mode == KnobMode.VOLUME -> MaterialTheme.colorScheme.tertiary
        mode == KnobMode.LATENCY -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val knobEdgeColor = MaterialTheme.colorScheme.tertiary
    val indicatorColor = when (mode) {
        KnobMode.LATENCY -> MaterialTheme.colorScheme.onPrimaryContainer
        KnobMode.VOLUME -> MaterialTheme.colorScheme.onTertiary
    }
    val centerTextColor = when (mode) {
        KnobMode.LATENCY -> MaterialTheme.colorScheme.onSecondaryContainer
        KnobMode.VOLUME -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Box(
        modifier = Modifier
            .size(knobSize)
            .onSizeChanged { size ->
                center = Offset(size.width / 2f, size.height / 2f)
                knobRadiusPx = minOf(size.width, size.height) / 2f
            }
            .pointerInput(mode) {
                detectTapGestures(
                    onTap = { onModeToggle() },
                    onDoubleTap = {
                        isActive = false
                        previousSmoothedDeltaDeg = 0f
                        accumulatedDelta = 0f
                        when (mode) {
                            KnobMode.LATENCY -> if (displayLatency != 0) {
                                displayLatency = 0
                                onLatencyChange(0)
                            }

                            KnobMode.VOLUME -> onMutedToggle()
                        }
                    },
                )
            }
            .pointerInput(mode) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { _ ->
                        isActive = true
                        previousSmoothedDeltaDeg = 0f
                        accumulatedDelta = 0f
                    },
                    onDragEnd = {
                        isActive = false
                        previousSmoothedDeltaDeg = 0f
                        accumulatedDelta = 0f
                    },
                    onDragCancel = {
                        isActive = false
                        previousSmoothedDeltaDeg = 0f
                        accumulatedDelta = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val normalizedDeltaDeg = computeNormalizedAngularDelta(
                            center = center,
                            pointerPosition = change.position,
                            dragAmount = dragAmount,
                            knobRadiusPx = knobRadiusPx,
                            minActiveRadiusRatio = minActiveRadiusRatio,
                            tinyMotionDeadzoneDeg = tinyMotionDeadzoneDeg,
                            maxEventDeltaDeg = maxEventDeltaDeg,
                        )

                        val smoothedDeltaDeg =
                            (previousSmoothedDeltaDeg * smoothingPrevWeight) +
                                (normalizedDeltaDeg * smoothingCurrentWeight)
                        previousSmoothedDeltaDeg = smoothedDeltaDeg
                        accumulatedDelta += smoothedDeltaDeg * angularGain

                        while (accumulatedDelta >= degreesPerStep) {
                            when (mode) {
                                KnobMode.LATENCY -> if (displayLatency < maxLatency) {
                                    val v = (displayLatency + 1).coerceAtMost(maxLatency)
                                    displayLatency = v
                                    onLatencyChange(v)
                                }

                                KnobMode.VOLUME -> if (displayVolume < 100) {
                                    val v = (displayVolume + 1).coerceAtMost(100)
                                    displayVolume = v
                                    onVolumeChange(v)
                                }
                            }
                            accumulatedDelta -= degreesPerStep
                        }

                        while (accumulatedDelta <= -degreesPerStep) {
                            when (mode) {
                                KnobMode.LATENCY -> if (displayLatency > minLatency) {
                                    val v = (displayLatency - 1).coerceAtLeast(minLatency)
                                    displayLatency = v
                                    onLatencyChange(v)
                                }

                                KnobMode.VOLUME -> if (displayVolume > 0) {
                                    val v = (displayVolume - 1).coerceAtLeast(0)
                                    displayVolume = v
                                    onVolumeChange(v)
                                }
                            }
                            accumulatedDelta += degreesPerStep
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(knobSize * indicatorScale),
        ) {
            val radius = size.minDimension / 2f
            val edgeStroke = size.minDimension * 0.045f
            val shadowRadius = radius * 0.86f

            val indicatorAngle = when (mode) {
                KnobMode.LATENCY -> {
                    val latencyRange = (maxLatency - minLatency).toFloat()
                    val normalized = (displayLatency - minLatency).toFloat() / latencyRange
                    val zeroNormalized = (0 - minLatency).toFloat() / latencyRange
                    (normalized - zeroNormalized) * 360f
                }

                KnobMode.VOLUME -> {
                    // 270° arc: 0% ≈ 7 o'clock (-135°), 50% = top (0°), 100% ≈ 5 o'clock (135°)
                    -135f + (displayVolume / 100f) * 270f
                }
            }

            val indicatorRadians = ((indicatorAngle - 90f) * PI / 180f).toFloat()
            val indicatorRadius = radius * 0.67f
            val indicatorCenter = Offset(
                x = center.x + cos(indicatorRadians) * indicatorRadius,
                y = center.y + sin(indicatorRadians) * indicatorRadius,
            )

            drawCircle(color = knobFillColor, radius = shadowRadius)
            drawCircle(
                color = knobEdgeColor,
                radius = shadowRadius,
                style = Stroke(width = edgeStroke),
            )
            drawCircle(
                color = knobEdgeColor.copy(alpha = 0.14f),
                radius = shadowRadius * 0.76f,
            )
            drawCircle(
                color = indicatorColor,
                radius = size.minDimension * 0.055f,
                center = indicatorCenter,
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (mode == KnobMode.LATENCY) "LAT" else "VOL",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = centerTextColor.copy(alpha = 0.5f),
            )
            when (mode) {
                KnobMode.LATENCY -> {
                    Text(
                        text = "$displayLatency",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = centerTextColor,
                    )
                    Text(
                        text = "ms",
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        color = centerTextColor.copy(alpha = 0.8f),
                    )
                }

                KnobMode.VOLUME -> {
                    Text(
                        text = "$displayVolume%",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = centerTextColor.copy(alpha = if (muted) 0.4f else 1f),
                    )
                    Text(
                        text = if (muted) "muted" else " ",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = centerTextColor.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

private fun computeNormalizedAngularDelta(
    center: Offset,
    pointerPosition: Offset,
    dragAmount: Offset,
    knobRadiusPx: Float,
    minActiveRadiusRatio: Float,
    tinyMotionDeadzoneDeg: Float,
    maxEventDeltaDeg: Float,
): Float {
    val touchVector = pointerPosition - center
    val radius = sqrt((touchVector.x * touchVector.x) + (touchVector.y * touchVector.y))
    val minActiveRadiusPx = knobRadiusPx * minActiveRadiusRatio

    if (radius < minActiveRadiusPx || radius <= 0f) {
        return 0f
    }

    val tangentUnit = Offset(
        x = -touchVector.y / radius,
        y = touchVector.x / radius,
    )
    val tangentialPx = (dragAmount.x * tangentUnit.x) + (dragAmount.y * tangentUnit.y)
    val angularDeltaDeg = (tangentialPx / radius) * (180f / PI.toFloat())

    if (abs(angularDeltaDeg) < tinyMotionDeadzoneDeg) {
        return 0f
    }

    return angularDeltaDeg.coerceIn(-maxEventDeltaDeg, maxEventDeltaDeg)
}

val mockSnapcastGroups = listOf(
    Group(
        clients = listOf(
            Client(
                config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                connected = true,
                host = Host("", "", "", "LEFT Device 1", ""),
                id = "LEFT Device 1",
                lastSeen = LastSeen(0, 0),
                snapclient = SnapClient("Snapclient", 2, "0.34.0"),
            ),
            Client(
                config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                connected = true,
                host = Host("", "", "", "LEFT Device 2", ""),
                id = "LEFT Device 2",
                lastSeen = LastSeen(0, 0),
                snapclient = SnapClient("Snapclient", 2, "0.34.0"),
            ),
            Client(
                config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                connected = true,
                host = Host("", "", "", "LEFT Device 3", ""),
                id = "LEFT Device 3",
                lastSeen = LastSeen(0, 0),
                snapclient = SnapClient("Snapclient", 2, "0.34.0"),
            ),
            Client(
                config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                connected = true,
                host = Host("", "", "", "LEFT Device 4", ""),
                id = "LEFT Device 4",
                lastSeen = LastSeen(0, 0),
                snapclient = SnapClient("Snapclient", 2, "0.34.0"),
            ),
            Client(
                config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                connected = true,
                host = Host("", "", "", "LEFT Device 5", ""),
                id = "LEFT Device 5",
                lastSeen = LastSeen(0, 0),
                snapclient = SnapClient("Snapclient", 2, "0.34.0"),
            ),
        ),
        id = "left_group",
        muted = false,
        name = "LEFT",
        streamId = "left_stream",
    ),
    Group(
        clients = listOf(
            Client(
                config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                connected = true,
                host = Host("", "", "", "RIGHT Device 1", ""),
                id = "RIGHT Device 1",
                lastSeen = LastSeen(0, 0),
                snapclient = SnapClient("Snapclient", 2, "0.34.0"),
            ),
            Client(
                config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                connected = true,
                host = Host("", "", "", "RIGHT Device 2", ""),
                id = "RIGHT Device 2",
                lastSeen = LastSeen(0, 0),
                snapclient = SnapClient("Snapclient", 2, "0.34.0"),
            ),
            Client(
                config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                connected = true,
                host = Host("", "", "", "RIGHT Device 3", ""),
                id = "RIGHT Device 3",
                lastSeen = LastSeen(0, 0),
                snapclient = SnapClient("Snapclient", 2, "0.34.0"),
            ),
            Client(
                config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                connected = true,
                host = Host("", "", "", "RIGHT Device 4", ""),
                id = "RIGHT Device 4",
                lastSeen = LastSeen(0, 0),
                snapclient = SnapClient("Snapclient", 2, "0.34.0"),
            ),
            Client(
                config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                connected = true,
                host = Host("", "", "", "RIGHT Device 5", ""),
                id = "RIGHT Device 5",
                lastSeen = LastSeen(0, 0),
                snapclient = SnapClient("Snapclient", 2, "0.34.0"),
            ),
        ),
        id = "right_group",
        muted = false,
        name = "RIGHT",
        streamId = "right_stream",
    ),
)

@Preview(
    showBackground = true,
    widthDp = 320,
    uiMode = UI_MODE_NIGHT_YES,
    name = "DefaultPreviewDark",
)
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, widthDp = 360, name = "DefaultPreviewWide")
@Composable
fun DefaultPreview() {
    RadioTheme(
        schemeChoice = SchemeChoice.ORANGE,
    ) {
        SnapserverGroups(
            groups = mockSnapcastGroups,
            onClientVolumeChange = { _, _, _ -> },
            onClientLatencyChange = { _, _ -> },
        )
    }
}
