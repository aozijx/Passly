package com.aozijx.passly.presentation.feature.vault.list.component.fab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.animation.SharedTransitionOverlayClip
import com.aozijx.passly.core.ui.animation.withSharedTransitionVisualOverflow
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.presentation.feature.vault.list.labelRes
import com.aozijx.passly.presentation.feature.vault.editor.common.ADD_ENTRY_FAB_SHARED_KEY
import com.aozijx.passly.presentation.feature.vault.editor.common.AddEntryFabVisualOverflow
import kotlinx.coroutines.delay

private const val FAB_MENU_STAGGER_MILLIS = 45

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
    val motionScheme = MaterialTheme.motionScheme

    val rotation by animateFloatAsState(
        targetValue = if (showFabMenu) 45f else 0f,
        animationSpec = motionScheme.defaultSpatialSpec(),
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
        enter = fadeIn(animationSpec = motionScheme.fastEffectsSpec()) +
                slideInVertically(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    initialOffsetY = { it / 4 }
                ),
        exit = fadeOut(animationSpec = motionScheme.fastEffectsSpec()) +
                slideOutVertically(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    targetOffsetY = { it / 4 }
                )
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 16.dp)
        ) {
            AnimatedVisibility(
                visible = showFabMenu,
                enter = EnterTransition.None,
                exit = fadeOut(animationSpec = motionScheme.fastEffectsSpec())
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    val itemCount = fabMenuOptions.size + 1

                    StaggeredFabMenuItem(
                        visible = showFabMenu,
                        index = 0,
                        itemCount = itemCount
                    ) { enabled ->
                        FabMenuChip(
                            label = stringResource(R.string.more),
                            icon = Icons.Default.MoreHoriz,
                            enabled = enabled,
                            onClick = {
                                showFabMenu = false
                                showAddEntryBottomSheet = true
                            }
                        )
                    }
                    fabMenuOptions.forEachIndexed { index, type ->
                        StaggeredFabMenuItem(
                            visible = showFabMenu,
                            index = index + 1,
                            itemCount = itemCount
                        ) { enabled ->
                            FabMenuChip(
                                label = stringResource(type.labelRes),
                                icon = type.icon(),
                                enabled = enabled,
                                onClick = {
                                    showFabMenu = false
                                    onAddTypeSelected(type)
                                }
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { showFabMenu = !showFabMenu },
                modifier = Modifier
                    .withSharedTransitionVisualOverflow(
                        sharedModifier = sharedFabModifier,
                        visualOverflow = AddEntryFabVisualOverflow
                    )
                    .size(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add),
                    modifier = Modifier.rotate(rotation)
                )
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
private fun StaggeredFabMenuItem(
    visible: Boolean,
    index: Int,
    itemCount: Int,
    content: @Composable (enabled: Boolean) -> Unit
) {
    val motionScheme = MaterialTheme.motionScheme
    var itemVisible by remember { mutableStateOf(false) }

    LaunchedEffect(visible, index, itemCount) {
        val staggerIndex = if (visible) itemCount - index - 1 else index
        delay((staggerIndex * FAB_MENU_STAGGER_MILLIS).toLong())
        itemVisible = visible
    }

    val alpha by animateFloatAsState(
        targetValue = if (itemVisible) 1f else 0f,
        animationSpec = motionScheme.fastEffectsSpec(),
        label = "fabMenuItemAlpha"
    )
    val horizontalOffsetFraction by animateFloatAsState(
        targetValue = if (itemVisible) 0f else 0.5f,
        animationSpec = motionScheme.defaultSpatialSpec(),
        label = "fabMenuItemOffset"
    )
    val interactionEnabled = visible && itemVisible

    Box(
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
                translationX = size.width * horizontalOffsetFraction
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .then(
                if (interactionEnabled) Modifier else Modifier.clearAndSetSemantics { }
            )
    ) {
        content(interactionEnabled)
    }
}

@Composable
private fun FabMenuChip(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
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
