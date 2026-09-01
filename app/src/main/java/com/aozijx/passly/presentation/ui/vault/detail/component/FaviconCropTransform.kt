package com.aozijx.passly.presentation.ui.vault.detail.component

import kotlin.math.max

internal data class FaviconCropGeometry(
    val viewportWidth: Float = 0f,
    val viewportHeight: Float = 0f,
    val imageWidth: Float = 0f,
    val imageHeight: Float = 0f,
)

internal data class FaviconCropGestureState(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

internal data class FaviconCropSelection(
    val zoom: Float,
    val offsetX: Float,
    val offsetY: Float,
)

internal object FaviconCropTransform {
    private const val MIN_ZOOM = 1f
    private const val MAX_ZOOM = 6f

    fun applyGesture(
        state: FaviconCropGestureState,
        geometry: FaviconCropGeometry,
        panX: Float,
        panY: Float,
        zoomChange: Float,
    ): FaviconCropGestureState {
        val zoom = (state.zoom * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
        return clamp(
            state.copy(
                zoom = zoom,
                offsetX = state.offsetX + panX,
                offsetY = state.offsetY + panY,
            ),
            geometry,
        )
    }

    fun clamp(
        state: FaviconCropGestureState,
        geometry: FaviconCropGeometry,
    ): FaviconCropGestureState {
        val bounds = translationBounds(geometry, state.zoom)
        return state.copy(
            offsetX = state.offsetX.coerceIn(-bounds.first, bounds.first),
            offsetY = state.offsetY.coerceIn(-bounds.second, bounds.second),
        )
    }

    fun toRequest(
        state: FaviconCropGestureState,
        geometry: FaviconCropGeometry,
    ): FaviconCropSelection {
        val clamped = clamp(state, geometry)
        val bounds = translationBounds(geometry, clamped.zoom)
        return FaviconCropSelection(
            zoom = clamped.zoom,
            offsetX = clamped.offsetX.normalizedBy(bounds.first),
            offsetY = clamped.offsetY.normalizedBy(bounds.second),
        )
    }

    private fun translationBounds(geometry: FaviconCropGeometry, zoom: Float): Pair<Float, Float> {
        if (
            geometry.viewportWidth <= 0f || geometry.viewportHeight <= 0f ||
            geometry.imageWidth <= 0f || geometry.imageHeight <= 0f
        ) {
            return 0f to 0f
        }
        val baseScale = max(
            geometry.viewportWidth / geometry.imageWidth,
            geometry.viewportHeight / geometry.imageHeight,
        )
        val renderedWidth = geometry.imageWidth * baseScale * zoom
        val renderedHeight = geometry.imageHeight * baseScale * zoom
        return ((renderedWidth - geometry.viewportWidth) / 2f).coerceAtLeast(0f) to
            ((renderedHeight - geometry.viewportHeight) / 2f).coerceAtLeast(0f)
    }

    private fun Float.normalizedBy(maximum: Float): Float =
        if (maximum <= 0f) 0f else (this / maximum).coerceIn(-1f, 1f)
}
