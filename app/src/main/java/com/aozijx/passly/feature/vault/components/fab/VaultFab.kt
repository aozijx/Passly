package com.aozijx.passly.feature.vault.components.fab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.animation.SharedTransitionOverlayClip
import com.aozijx.passly.core.ui.animation.withSharedTransitionVisualOverflow
import com.aozijx.passly.feature.vault.editor.common.ADD_ENTRY_FAB_SHARED_KEY
import com.aozijx.passly.feature.vault.editor.common.AddEntryFabVisualOverflow
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.presentation.labelRes

@Composable
fun VaultFab(
    onAddTypeSelected: (AddType) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    isVisible: Boolean = true
) {
    var showFabMenu by remember { mutableStateOf(false) }
    var showAddEntryBottomSheet by remember { mutableStateOf(false) }
    var pendingSheetSelection by remember { mutableStateOf<AddType?>(null) }
    val fabShape = MaterialTheme.shapes.extraLarge

    val rotation by animateFloatAsState(
        targetValue = if (showFabMenu) 45f else 0f,
        label = "fabRotation"
    )

    val fabMenuOptions = AddType.fabMenuOptions
    val sharedFabModifier = with(sharedTransitionScope) {
        Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(ADD_ENTRY_FAB_SHARED_KEY),
            animatedVisibilityScope = animatedVisibilityScope,
            resizeMode = RemeasureToBounds,
            clipInOverlayDuringTransition = SharedTransitionOverlayClip.None
        )
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) showFabMenu = false
    }

    LaunchedEffect(showAddEntryBottomSheet, pendingSheetSelection) {
        val selectedType = pendingSheetSelection ?: return@LaunchedEffect
        if (!showAddEntryBottomSheet) {
            pendingSheetSelection = null
            onAddTypeSelected(selectedType)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(transformOrigin = TransformOrigin(1f, 1f)),
        exit = fadeOut() + scaleOut(transformOrigin = TransformOrigin(1f, 1f))
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 16.dp, end = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FabMenuItemWithSpring(
                    visible = showFabMenu,
                    label = stringResource(R.string.more),
                    icon = Icons.Default.MoreHoriz,
                    onClick = {
                        showFabMenu = false
                        showAddEntryBottomSheet = true
                    }
                )

                fabMenuOptions.forEach { type ->
                    FabMenuItemWithSpring(
                        visible = showFabMenu,
                        label = stringResource(type.labelRes),
                        icon = type.icon(),
                        onClick = {
                            showFabMenu = false
                            onAddTypeSelected(type)
                        }
                    )
                }
            }

            Surface(
                onClick = { showFabMenu = !showFabMenu },
                modifier = Modifier
                    .withSharedTransitionVisualOverflow(
                        sharedModifier = sharedFabModifier,
                        visualOverflow = AddEntryFabVisualOverflow
                    )
                    .size(56.dp),
                shape = fabShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add),
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotation)
                    )
                }
            }
        }
    }

    if (showAddEntryBottomSheet) {
        AddEntryBottomSheet(
            onDismiss = { showAddEntryBottomSheet = false },
            onSelectType = { type ->
                showFabMenu = false
                pendingSheetSelection = type
                showAddEntryBottomSheet = false
            }
        )
    }
}

@Composable
fun FabMenuItemWithSpring(
    visible: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally { it / 2 } + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + slideOutHorizontally { it / 2 } + scaleOut(targetScale = 0.8f)
    ) {
        FabMenuItem(label = label, icon = icon, onClick = onClick)
    }
}

@Composable
fun FabMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
