package com.aozijx.passly.core.ui.components.widgets

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class SwipeActionSpec(
    val icon: ImageVector,
    val backgroundColor: Color,
    val iconTint: Color,
    val onAction: () -> Unit,
)

private const val SWIPE_REVEAL_WIDTH_FRACTION = 0.28f
private const val SWIPE_TRIGGER_WIDTH_FRACTION = 0.22f
private const val SWIPE_TRIGGER_REVEAL_FRACTION = 0.9f
private const val SWIPE_OVERFLOW_RESISTANCE = 0.22f
private const val SWIPE_OVERFLOW_CAP = 1.35f
private const val SWIPE_RESISTANCE_POWER = 0.82f
private const val SWIPE_SETTLED_EPSILON = 0.5f

@Composable
fun SwipeActionContainer(
    leftAction: SwipeActionSpec?,
    rightAction: SwipeActionSpec?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    foregroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val currentLeftAction = rememberUpdatedState(leftAction)
    val currentRightAction = rememberUpdatedState(rightAction)
    val settleSpec =
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )

    var widthPx by remember { mutableFloatStateOf(0f) }
    val maxSwipePx =
        remember(widthPx) {
            calculateSwipeRevealDistance(containerWidth = widthPx)
        }
    val triggerThresholdPx =
        remember(widthPx, maxSwipePx) {
            calculateSwipeTriggerThreshold(
                containerWidth = widthPx,
                revealDistance = maxSwipePx,
            )
        }
    var rawDragOffset by remember { mutableFloatStateOf(0f) }
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    var thresholdHapticSent by remember { mutableStateOf(false) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    fun settleToRest() {
        settleJob?.cancel()
        settleJob =
            scope.launch {
                val initialOffset = swipeOffset
                rawDragOffset = 0f
                if (abs(initialOffset) <= SWIPE_SETTLED_EPSILON) {
                    swipeOffset = 0f
                    return@launch
                }
                animate(
                    initialValue = initialOffset,
                    targetValue = 0f,
                    animationSpec = settleSpec,
                ) { value, _ ->
                    swipeOffset = value
                }
                swipeOffset = 0f
            }
    }

    LaunchedEffect(enabled) {
        if (!enabled) {
            thresholdHapticSent = false
            settleToRest()
        }
    }

    val dragModifier =
        Modifier.pointerInput(
            enabled,
            leftAction,
            rightAction,
            maxSwipePx,
            triggerThresholdPx,
        ) {
            if (!enabled) return@pointerInput

            detectHorizontalDragGestures(
                onDragStart = {
                    settleJob?.cancel()
                    rawDragOffset = swipeOffset
                    thresholdHapticSent = false
                },
                onDragCancel = {
                    thresholdHapticSent = false
                    settleToRest()
                },
                onDragEnd = {
                    val action =
                        when {
                            swipeOffset <= -triggerThresholdPx -> currentLeftAction.value
                            swipeOffset >= triggerThresholdPx -> currentRightAction.value
                            else -> null
                        }
                    thresholdHapticSent = false
                    if (action != null) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    action?.onAction?.invoke()
                    settleToRest()
                },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    val minOffset = if (currentLeftAction.value != null) -maxSwipePx else 0f
                    val maxOffset = if (currentRightAction.value != null) maxSwipePx else 0f
                    rawDragOffset =
                        accumulateSwipeRawOffset(
                            currentRawOffset = rawDragOffset,
                            dragAmount = dragAmount,
                            minOffset = minOffset,
                            maxOffset = maxOffset,
                        )
                    swipeOffset =
                        calculateSwipeVisualOffset(
                            rawOffset = rawDragOffset,
                            minOffset = minOffset,
                            maxOffset = maxOffset,
                        )

                    val thresholdReached = abs(swipeOffset) >= triggerThresholdPx
                    if (thresholdReached && !thresholdHapticSent) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    thresholdHapticSent = thresholdReached
                },
            )
        }

    val activeAction =
        when {
            swipeOffset > SWIPE_SETTLED_EPSILON -> rightAction
            swipeOffset < -SWIPE_SETTLED_EPSILON -> leftAction
            else -> null
        }
    val progress = calculateSwipeProgress(swipeOffset, triggerThresholdPx)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .onSizeChanged { widthPx = it.width.toFloat() }
                .then(dragModifier),
    ) {
        ActionBackground(
            action = activeAction,
            offset = swipeOffset,
            progress = progress,
            shape = shape,
        )

        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(swipeOffset.roundToInt(), 0) },
            shape = shape,
            color = foregroundColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            content()
        }
    }
}

@Composable
private fun BoxScope.ActionBackground(
    action: SwipeActionSpec?,
    offset: Float,
    progress: Float,
    shape: Shape,
) {
    if (action == null) return

    Box(
        modifier =
            Modifier
                .matchParentSize()
                .clip(shape)
                .background(action.backgroundColor),
        contentAlignment = if (offset > 0f) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = null,
            tint = action.iconTint.copy(alpha = progress),
            modifier =
                Modifier
                    .padding(horizontal = 24.dp)
                    .size(24.dp)
                    .graphicsLayer {
                        alpha = progress
                        val scale = 0.82f + progress * 0.18f
                        scaleX = scale
                        scaleY = scale
                    },
        )
    }
}

internal fun calculateSwipeRevealDistance(containerWidth: Float): Float {
    if (containerWidth <= 0f) return 0f
    return containerWidth * SWIPE_REVEAL_WIDTH_FRACTION
}

internal fun calculateSwipeTriggerThreshold(
    containerWidth: Float,
    revealDistance: Float,
): Float {
    if (containerWidth <= 0f || revealDistance <= 0f) return 0f

    return min(
        containerWidth * SWIPE_TRIGGER_WIDTH_FRACTION,
        revealDistance * SWIPE_TRIGGER_REVEAL_FRACTION,
    )
}

internal fun calculateSwipeVisualOffset(
    rawOffset: Float,
    minOffset: Float,
    maxOffset: Float,
): Float {
    if (rawOffset == 0f) return 0f

    val directionLimit = if (rawOffset > 0f) maxOffset else -minOffset
    if (directionLimit <= 0f) return 0f

    val sign = if (rawOffset > 0f) 1f else -1f
    val absoluteRaw = abs(rawOffset)
    val resisted =
        if (absoluteRaw <= directionLimit) {
            val progress = (absoluteRaw / directionLimit).coerceIn(0f, 1f)
            directionLimit * (1f - (1f - progress).pow(SWIPE_RESISTANCE_POWER))
        } else {
            val overflow = absoluteRaw - directionLimit
            directionLimit + overflow * SWIPE_OVERFLOW_RESISTANCE
        }
    val capped = min(resisted, directionLimit * SWIPE_OVERFLOW_CAP)

    return sign * capped
}

internal fun accumulateSwipeRawOffset(
    currentRawOffset: Float,
    dragAmount: Float,
    minOffset: Float,
    maxOffset: Float,
): Float =
    (currentRawOffset + dragAmount).coerceIn(
        minimumValue = if (minOffset < 0f) Float.NEGATIVE_INFINITY else 0f,
        maximumValue = if (maxOffset > 0f) Float.POSITIVE_INFINITY else 0f,
    )

internal fun calculateSwipeProgress(
    offset: Float,
    triggerThreshold: Float,
): Float {
    if (triggerThreshold <= 0f) return 0f
    return (abs(offset) / triggerThreshold).coerceIn(0f, 1f)
}
