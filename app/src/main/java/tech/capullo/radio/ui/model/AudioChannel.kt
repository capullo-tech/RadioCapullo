package tech.capullo.radio.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

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
