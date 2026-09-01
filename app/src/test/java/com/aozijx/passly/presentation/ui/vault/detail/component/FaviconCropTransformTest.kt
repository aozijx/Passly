package com.aozijx.passly.presentation.ui.vault.detail.component

import org.junit.Assert.assertEquals
import org.junit.Test

class FaviconCropTransformTest {

    @Test
    fun landscapeImage_clampsHorizontalPanAndNormalizesIt() {
        val geometry = FaviconCropGeometry(
            viewportWidth = 300f,
            viewportHeight = 300f,
            imageWidth = 600f,
            imageHeight = 300f,
        )

        val state = FaviconCropTransform.applyGesture(
            state = FaviconCropGestureState(),
            geometry = geometry,
            panX = 1_000f,
            panY = 1_000f,
            zoomChange = 1f,
        )
        val request = FaviconCropTransform.toRequest(state, geometry)

        assertEquals(150f, state.offsetX, 0.001f)
        assertEquals(0f, state.offsetY, 0.001f)
        assertEquals(1f, request.offsetX, 0.001f)
        assertEquals(0f, request.offsetY, 0.001f)
    }

    @Test
    fun portraitImage_clampsVerticalPanAndNormalizesIt() {
        val geometry = FaviconCropGeometry(
            viewportWidth = 240f,
            viewportHeight = 240f,
            imageWidth = 300f,
            imageHeight = 600f,
        )

        val state = FaviconCropTransform.applyGesture(
            state = FaviconCropGestureState(),
            geometry = geometry,
            panX = -1_000f,
            panY = -1_000f,
            zoomChange = 1f,
        )
        val request = FaviconCropTransform.toRequest(state, geometry)

        assertEquals(0f, state.offsetX, 0.001f)
        assertEquals(-120f, state.offsetY, 0.001f)
        assertEquals(0f, request.offsetX, 0.001f)
        assertEquals(-1f, request.offsetY, 0.001f)
    }

    @Test
    fun zoomAndPan_areClampedAgainstMeasuredGeometry() {
        val geometry = FaviconCropGeometry(200f, 200f, 200f, 200f)

        val state = FaviconCropTransform.applyGesture(
            state = FaviconCropGestureState(zoom = 5f, offsetX = 350f, offsetY = -350f),
            geometry = geometry,
            panX = 500f,
            panY = -500f,
            zoomChange = 2f,
        )
        val request = FaviconCropTransform.toRequest(state, geometry)

        assertEquals(6f, state.zoom, 0.001f)
        assertEquals(500f, state.offsetX, 0.001f)
        assertEquals(-500f, state.offsetY, 0.001f)
        assertEquals(1f, request.offsetX, 0.001f)
        assertEquals(-1f, request.offsetY, 0.001f)
    }
}
