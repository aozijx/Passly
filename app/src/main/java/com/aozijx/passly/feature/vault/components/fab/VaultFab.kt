package com.aozijx.passly.feature.vault.components.fab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.theme.PasslyTheme
import com.aozijx.passly.feature.vault.model.AddType
import kotlinx.coroutines.delay

@Composable
fun VaultFab(
    onAddTypeSelected: (AddType) -> Unit,
    isVisible: Boolean = true
) {
    var showFabMenu by remember { mutableStateOf(false) }
    var showAddEntrySheet by remember { mutableStateOf(false) }
    val expressive = PasslyTheme.isExpressive
    val motionScheme = MaterialTheme.motionScheme

    val rotation by animateFloatAsState(
        targetValue = if (showFabMenu) 45f else 0f,
        animationSpec = motionScheme.defaultSpatialSpec(),
        label = "fabRotation"
    )

    // 用于实现依次弹出的状态控制
    val fabMenuOptions = AddType.fabMenuOptions
    var visibleStates by remember { mutableStateOf(List(fabMenuOptions.size) { false }) }

    // 监听 showFabMenu 变化，手动控制延迟实现交错效果
    LaunchedEffect(showFabMenu) {
        if (showFabMenu) {
            val lastIndex = fabMenuOptions.lastIndex
            for (i in lastIndex downTo 0) {
                visibleStates = visibleStates.toMutableList().apply { this[i] = true }
                if (i > 0) delay(60)
            }
        } else {
            visibleStates = visibleStates.toMutableList().apply { replaceAll { false } }
        }
    }

    if (!isVisible) {
        showFabMenu = false
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = motionScheme.fastEffectsSpec()) +
                scaleIn(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    transformOrigin = TransformOrigin(1f, 1f)
                ),
        exit = fadeOut(animationSpec = motionScheme.fastEffectsSpec()) +
                scaleOut(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    transformOrigin = TransformOrigin(1f, 1f)
                )
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                fabMenuOptions.forEachIndexed { index, type ->
                    FabMenuItemWithSpring(
                        visible = visibleStates[index],
                        label = stringResource(type.labelRes),
                        icon = type.icon,
                        onClick = {
                            showFabMenu = false
                            onAddTypeSelected(type)
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(if (expressive) 64.dp else 56.dp)
                    .shadow(4.dp, MaterialTheme.shapes.large)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showFabMenu = !showFabMenu },
                            onLongPress = { showAddEntrySheet = true }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add),
                    modifier = Modifier.rotate(rotation),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    if (showAddEntrySheet) {
        AddEntrySheet(
            onDismiss = { showAddEntrySheet = false },
            onSelectType = { type ->
                showFabMenu = false
                onAddTypeSelected(type)
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
    val motionScheme = MaterialTheme.motionScheme
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = motionScheme.fastEffectsSpec()) +
                slideInHorizontally(
                    animationSpec = motionScheme.defaultSpatialSpec()
                ) { it / 2 } +
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = motionScheme.defaultSpatialSpec()
                ),
        exit = fadeOut(animationSpec = motionScheme.fastEffectsSpec()) +
                scaleOut(
                    targetScale = 0.8f,
                    animationSpec = motionScheme.defaultSpatialSpec()
                )
    ) {
        FabMenuItem(label = label, icon = icon, onClick = onClick)
    }
}

@Composable
fun FabMenuItem(
    label: String, icon: ImageVector, onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        modifier = Modifier.height(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
