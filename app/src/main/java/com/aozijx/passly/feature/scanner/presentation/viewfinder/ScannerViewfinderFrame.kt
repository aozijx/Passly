package com.aozijx.passly.feature.scanner.presentation.viewfinder

import android.graphics.RectF
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val VIEWFINDER_WIDTH_RATIO = 0.72f
private const val VIEWFINDER_HEIGHT_RATIO = 0.48f
private val VIEWFINDER_MAX_SIZE = 340.dp

internal data class ScannerViewfinderFrame(
    val size: Dp,
    val sizePx: Float,
    val boundsInPreview: RectF,
) {
    companion object {
        fun fitContainer(
            containerWidth: Dp,
            containerHeight: Dp,
            density: Density,
        ): ScannerViewfinderFrame {
            val size =
                minOf(
                    containerWidth * VIEWFINDER_WIDTH_RATIO,
                    containerHeight * VIEWFINDER_HEIGHT_RATIO,
                    VIEWFINDER_MAX_SIZE,
                )
            val sizePx = with(density) { size.toPx() }
            val leftPx = with(density) { ((containerWidth - size) / 2).toPx() }
            val topPx = with(density) { ((containerHeight - size) / 2).toPx() }
            return ScannerViewfinderFrame(
                size = size,
                sizePx = sizePx,
                boundsInPreview = RectF(leftPx, topPx, leftPx + sizePx, topPx + sizePx),
            )
        }
    }
}
