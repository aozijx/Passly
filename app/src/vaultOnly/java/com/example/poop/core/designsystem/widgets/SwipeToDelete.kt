package com.example.poop.core.designsystem.widgets

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SwipeToDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    cornerRadius: Dp = 16.dp,
    deleteThreshold: Float = 0.5f,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val componentWidthPx = remember { mutableFloatStateOf(0f) }
    val offset = remember { Animatable(0f) }
    val hapticFeedback = LocalHapticFeedback.current
    val hasTriggeredHaptic = remember { mutableStateOf(false) }

    val thresholdPx = remember(componentWidthPx.floatValue) {
        componentWidthPx.floatValue * deleteThreshold
    }

    val swipeFraction = if (thresholdPx > 0) {
        (-offset.value / thresholdPx).coerceIn(0f, 1f)
    } else 0f

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
                val newOffset = (offset.value + delta * dampingFactor).coerceIn(-componentWidthPx.floatValue, 0f)
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
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.error.copy(alpha = swipeFraction * 0.7f),
                            MaterialTheme.colorScheme.error.copy(alpha = swipeFraction * 0.3f),
                            Color.Transparent
                        ),
                        startX = Float.POSITIVE_INFINITY,
                        endX = 0f
                    )
                ),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onError.copy(alpha = swipeFraction),
                modifier = Modifier
                    .padding(end = 20.dp)
                    .size(28.dp)
                    .graphicsLayer {
                        val scale = 0.8f + (swipeFraction * 0.4f).coerceAtMost(0.4f)
                        scaleX = scale
                        scaleY = scale
                    }
            )
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
                            if (abs(current) >= thresholdPx) {
                                offset.animateTo(
                                    targetValue = -componentWidthPx.floatValue,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                                onDelete()
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
