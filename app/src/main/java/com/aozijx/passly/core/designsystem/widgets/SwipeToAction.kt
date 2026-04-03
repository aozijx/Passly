package com.aozijx.passly.core.designsystem.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultSummary
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

data class SwipeAction(
    val icon: ImageVector,
    val backgroundColor: Color,
    val iconTint: Color,
    val onAction: () -> Unit,
    val direction: SwipeDirection = SwipeDirection.LEFT
)

enum class SwipeDirection {
    LEFT,
    RIGHT
}

@Composable
fun SwipeToAction(
    actions: List<SwipeAction>,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    cornerRadius: Dp = 16.dp,
    actionThreshold: Float = 0.5f,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val componentWidthPx = remember { mutableFloatStateOf(0f) }
    val offset = remember { Animatable(0f) }
    val hapticFeedback = LocalHapticFeedback.current
    val hasTriggeredHaptic = remember { mutableStateOf(false) }

    val thresholdPx = remember(componentWidthPx.floatValue) {
        componentWidthPx.floatValue * actionThreshold
    }

    val leftAction = actions.find { it.direction == SwipeDirection.LEFT }
    val rightAction = actions.find { it.direction == SwipeDirection.RIGHT }

    val swipeFraction = if (thresholdPx > 0) {
        (abs(offset.value) / thresholdPx).coerceIn(0f, 1f)
    } else 0f

    val currentAction = remember(offset.value) {
        when {
            offset.value < 0 -> leftAction
            offset.value > 0 -> rightAction
            else -> null
        }
    }

    LaunchedEffect(swipeFraction) {
        if (swipeFraction >= 1f && !hasTriggeredHaptic.value) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            hasTriggeredHaptic.value = true
        } else if (swipeFraction < 1f) {
            hasTriggeredHaptic.value = false
        }
    }

    val draggableState = rememberDraggableState(
        onDelta = { delta ->
            scope.launch {
                val dampingFactor = 1f
                val newOffset = (offset.value + delta * dampingFactor).coerceIn(
                    -componentWidthPx.floatValue,
                    componentWidthPx.floatValue
                )
                offset.snapTo(newOffset)
            }
        }
    )

    LaunchedEffect(isActive) {
        if (!isActive && offset.value != 0f) {
            offset.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                componentWidthPx.floatValue = size.width.toFloat()
            }
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        if (currentAction != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = if (offset.value < 0) {
                                listOf(
                                    Color.Transparent,
                                    currentAction.backgroundColor.copy(alpha = swipeFraction * 0.3f),
                                    currentAction.backgroundColor.copy(alpha = swipeFraction * 0.7f)
                                )
                            } else {
                                listOf(
                                    currentAction.backgroundColor.copy(alpha = swipeFraction * 0.7f),
                                    currentAction.backgroundColor.copy(alpha = swipeFraction * 0.3f),
                                    Color.Transparent
                                )
                            }
                        )
                    ),
                contentAlignment = if (offset.value < 0) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Icon(
                    imageVector = currentAction.icon,
                    contentDescription = null,
                    tint = currentAction.iconTint.copy(alpha = swipeFraction),
                    modifier = Modifier
                        .padding(
                            end = if (offset.value < 0) 20.dp else 0.dp,
                            start = if (offset.value < 0) 0.dp else 20.dp
                        )
                        .size(24.dp)
                        .graphicsLayer {
                            val scale = 0.8f + (swipeFraction * 0.4f).coerceAtMost(0.4f)
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    enabled = isActive,
                    onDragStopped = {
                        scope.launch {
                            val current = offset.value
                            val action = when {
                                current < 0 && abs(current) >= thresholdPx -> leftAction
                                current > 0 && abs(current) >= thresholdPx -> rightAction
                                else -> null
                            }
                            if (action != null) {
                                action.onAction()
                                offset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                            } else {
                                offset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        }
                    }
                ),
            shape = RoundedCornerShape(cornerRadius),
            tonalElevation = 2.dp,
            shadowElevation = 2.dp
        ) {
            content()
        }
    }
}

fun createSwipeAction(
    actionType: SwipeActionType,
    direction: SwipeDirection,
    onAction: () -> Unit,
    backgroundColor: Color,
    iconTint: Color
): SwipeAction? {
    if (actionType == SwipeActionType.DISABLED || actionType.icon == null) return null
    return SwipeAction(
        icon = actionType.icon,
        backgroundColor = backgroundColor,
        iconTint = iconTint,
        onAction = onAction,
        direction = direction
    )
}

fun handleSwipeAction(
    actionType: SwipeActionType,
    item: VaultEntry,
    onAuthRequired: (onSuccess: () -> Unit) -> Unit,
    onQuickDelete: (VaultEntry) -> Unit,
    onCopyPassword: (decryptedPassword: String) -> Unit,
    onDecryptPassword: (onResult: (String?) -> Unit) -> Unit,
    onShowDetail: (VaultEntry) -> Unit
) {
    when (actionType) {
        SwipeActionType.DELETE -> {
            onAuthRequired { onQuickDelete(item) }
        }
        SwipeActionType.EDIT -> {
            // TODO: implement edit action
        }
        SwipeActionType.DETAIL -> {
            onShowDetail(item)
        }
        SwipeActionType.COPY_PASSWORD -> {
            onDecryptPassword { decryptedPassword ->
                decryptedPassword?.let { onCopyPassword(it) }
            }
        }
        SwipeActionType.DISABLED -> {
            // do nothing
        }
    }
}

fun handleSwipeAction(
    actionType: SwipeActionType,
    item: VaultSummary,
    onAuthRequired: (onSuccess: () -> Unit) -> Unit,
    onQuickDelete: (VaultSummary) -> Unit,
    onCopyPassword: (decryptedPassword: String) -> Unit,
    onDecryptPassword: (onResult: (String?) -> Unit) -> Unit,
    onShowDetail: (VaultSummary) -> Unit
) {
    when (actionType) {
        SwipeActionType.DELETE -> {
            onAuthRequired { onQuickDelete(item) }
        }
        SwipeActionType.EDIT -> {
            // TODO: implement edit action
        }
        SwipeActionType.DETAIL -> {
            onShowDetail(item)
        }
        SwipeActionType.COPY_PASSWORD -> {
            onDecryptPassword { decryptedPassword ->
                decryptedPassword?.let { onCopyPassword(it) }
            }
        }
        SwipeActionType.DISABLED -> {
            // do nothing
        }
    }
}


