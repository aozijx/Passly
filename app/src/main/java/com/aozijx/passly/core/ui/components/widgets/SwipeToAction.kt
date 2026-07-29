package com.aozijx.passly.core.ui.components.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.aozijx.passly.domain.settings.model.SwipeActionType
import kotlinx.coroutines.launch

data class SwipeAction(
    val icon: ImageVector,
    val backgroundColor: Color,
    val iconTint: Color,
    val onAction: () -> Unit,
    val direction: SwipeDirection = SwipeDirection.LEFT
)

enum class SwipeDirection {
    LEFT, RIGHT
}

@Composable
fun SwipeToAction(
    actions: List<SwipeAction>,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val leftAction = remember(actions) {
        actions.find { it.direction == SwipeDirection.LEFT }
    }
    val rightAction = remember(actions) {
        actions.find { it.direction == SwipeDirection.RIGHT }
    }
    val currentLeftAction = rememberUpdatedState(leftAction)
    val currentRightAction = rememberUpdatedState(rightAction)
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    LaunchedEffect(isActive) {
        if (!isActive && dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }

    val onDismiss = remember(dismissState, scope) {
        { direction: SwipeToDismissBoxValue ->
            val action = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> currentRightAction.value
                SwipeToDismissBoxValue.EndToStart -> currentLeftAction.value
                SwipeToDismissBoxValue.Settled -> null
            }
            action?.onAction?.invoke()
            scope.launch { dismissState.reset() }
            Unit
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
        enableDismissFromStartToEnd = rightAction != null,
        enableDismissFromEndToStart = leftAction != null,
        gesturesEnabled = isActive,
        onDismiss = onDismiss,
        backgroundContent = {
            val currentAction = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> rightAction
                SwipeToDismissBoxValue.EndToStart -> leftAction
                SwipeToDismissBoxValue.Settled -> null
            }
            val progress = dismissState.progress.coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(currentAction?.backgroundColor ?: Color.Transparent),
                contentAlignment = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    SwipeToDismissBoxValue.Settled -> Alignment.Center
                }
            ) {
                currentAction?.let { action ->
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        tint = action.iconTint.copy(alpha = progress),
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .size(24.dp)
                            .graphicsLayer {
                                val scale = 0.8f + progress * 0.2f
                                scaleX = scale
                                scaleY = scale
                            }
                    )
                }
            }
        },
        content = {
            content()
        }
    )
}

fun createSwipeAction(
    actionType: SwipeActionType,
    direction: SwipeDirection,
    onAction: () -> Unit,
    backgroundColor: Color,
    iconTint: Color
): SwipeAction {
    val icon = when (actionType) {
        SwipeActionType.DELETE -> Icons.Default.Delete
        SwipeActionType.DETAIL -> Icons.Default.Info
        SwipeActionType.COPY_PASSWORD -> Icons.Default.ContentCopy
        SwipeActionType.COPY_USERNAME -> Icons.Default.Person
    }
    return SwipeAction(
        icon = icon,
        backgroundColor = backgroundColor,
        iconTint = iconTint,
        onAction = onAction,
        direction = direction
    )
}
