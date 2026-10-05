package com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints

internal enum class VaultMenuPage { MAIN, SORT, CATEGORY_FILTER }

@Composable
internal fun VaultMenuPageTransition(
    currentPage: VaultMenuPage,
    mainContent: @Composable () -> Unit,
    sortContent: @Composable () -> Unit,
    categoryFilterContent: @Composable () -> Unit,
) {
    Layout(
        content = {
            MenuPageLayer(
                visible = currentPage == VaultMenuPage.MAIN,
                content = mainContent,
            )
            MenuPageLayer(
                visible = currentPage == VaultMenuPage.SORT,
                content = sortContent,
            )
            MenuPageLayer(
                visible = currentPage == VaultMenuPage.CATEGORY_FILTER,
                content = categoryFilterContent,
            )
        },
        measurePolicy = remember(currentPage) {
            TargetMenuPageMeasurePolicy(currentPage)
        },
    )
}

@Composable
private fun MenuPageLayer(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    val motionScheme = MaterialTheme.motionScheme
    Box {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = motionScheme.defaultEffectsSpec()),
            exit = fadeOut(animationSpec = motionScheme.defaultEffectsSpec()),
        ) {
            content()
        }
    }
}

private class TargetMenuPageMeasurePolicy(
    private val targetPage: VaultMenuPage,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val placeables = measurables.map { measurable ->
            val naturalWidth = measurable.maxIntrinsicWidth(constraints.maxHeight)
            measurable.measure(
                Constraints(
                    minWidth = naturalWidth,
                    maxWidth = naturalWidth,
                    minHeight = 0,
                    maxHeight = constraints.maxHeight,
                ),
            )
        }
        val targetIndex = targetPage.ordinal
        val target = placeables[targetIndex]
        val width = target.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = target.height.coerceIn(constraints.minHeight, constraints.maxHeight)

        return layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                if (index != targetIndex) placeable.placeRelative(0, 0)
            }
            target.placeRelative(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = target(measurables).maxIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = target(measurables).maxIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = target(measurables).minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = target(measurables).maxIntrinsicHeight(width)

    private fun target(measurables: List<IntrinsicMeasurable>): IntrinsicMeasurable =
        measurables[targetPage.ordinal]
}
