package com.aozijx.passly.feature.vault.editor.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

const val ADD_ENTRY_FAB_SHARED_KEY = "vault-add-entry-fab"
val AddEntryFabVisualOverflow = 12.dp

@Composable
fun SharedAddEntryExtendedFab(
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    expanded: Boolean = true,
    labelVisible: Boolean = expanded,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    ExtendedFloatingActionButton(
        onClick = { if (enabled) onClick() },
        expanded = expanded,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
        icon = icon,
        text = {
            AnimatedVisibility(
                visible = labelVisible,
                enter = fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                    expandHorizontally(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                    shrinkHorizontally(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec())
            ) {
                Text(
                    text = label,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }
    )
}

@Composable
fun rememberSharedFabLabelVisible(
    visible: Boolean
): Boolean {
    var labelVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        labelVisible = visible
    }
    return labelVisible
}
