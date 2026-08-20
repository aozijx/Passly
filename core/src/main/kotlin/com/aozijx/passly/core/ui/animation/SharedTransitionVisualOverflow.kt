package com.aozijx.passly.core.ui.animation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/** Common overlay clipping policies for shared transitions. */
object SharedTransitionOverlayClip {
    /** Do not inherit a clip path from a parent shared bounds. */
    val None: SharedTransitionScope.OverlayClip =
        object : SharedTransitionScope.OverlayClip {
            override fun getClipPath(
                sharedContentState: SharedTransitionScope.SharedContentState,
                bounds: Rect,
                layoutDirection: LayoutDirection,
                density: Density
            ): Path? = null
        }
}

/**
 * Includes drawing outside a composable's layout bounds, such as elevation shadows, in the shared
 * transition layer without changing the size reported to its parent.
 *
 * [sharedModifier] must contain `sharedBounds` or `sharedElement`. [visualOverflow] should cover
 * the largest shadow/blur radius used at either end of the transition. The modifier order is kept
 * here deliberately: compensating layout -> shared layer -> transparent overflow -> content.
 */
fun Modifier.withSharedTransitionVisualOverflow(
    sharedModifier: Modifier,
    visualOverflow: Dp
): Modifier =
    layout { measurable, constraints ->
        val inset = visualOverflow.roundToPx().coerceAtLeast(0)
        val extraSpace = inset * 2
        fun expanded(value: Int): Int =
            if (value == Constraints.Infinity) {
                Constraints.Infinity
            } else {
                (value.toLong() + extraSpace)
                    .coerceAtMost(Constraints.Infinity.toLong())
                    .toInt()
            }

        val placeable = measurable.measure(
            constraints.copy(
                minWidth = expanded(constraints.minWidth),
                maxWidth = expanded(constraints.maxWidth),
                minHeight = expanded(constraints.minHeight),
                maxHeight = expanded(constraints.maxHeight)
            )
        )
        val width = (placeable.width - extraSpace)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (placeable.height - extraSpace)
            .coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width, height) {
            placeable.placeRelative(-inset, -inset)
        }
    }
        .then(sharedModifier)
        .padding(visualOverflow)
