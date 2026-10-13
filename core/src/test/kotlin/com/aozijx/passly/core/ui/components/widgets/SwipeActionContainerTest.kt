package com.aozijx.passly.core.ui.components.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeActionContainerTest {

    @Test
    fun revealDistance_scalesWithContainerWidth() {
        val revealDistance = calculateSwipeRevealDistance(
            containerWidth = 400f
        )

        assertEquals(112f, revealDistance, 0.001f)
    }

    @Test
    fun revealDistance_returnsZeroBeforeMeasurement() {
        assertEquals(0f, calculateSwipeRevealDistance(containerWidth = 0f), 0.001f)
    }

    @Test
    fun triggerThreshold_scalesWithContainerWidthAndStaysInsideRevealDistance() {
        val triggerThreshold = calculateSwipeTriggerThreshold(
            containerWidth = 400f,
            revealDistance = 112f
        )

        assertEquals(88f, triggerThreshold, 0.001f)
        assertTrue(triggerThreshold < 112f)
    }

    @Test
    fun visualOffset_isResistedBeforeTheRevealLimit() {
        val rawOffset = 48f
        val visualOffset = calculateSwipeVisualOffset(
            rawOffset = rawOffset,
            minOffset = -96f,
            maxOffset = 96f
        )

        assertTrue(visualOffset > 0f)
        assertTrue(visualOffset < rawOffset)
    }

    @Test
    fun visualOffset_appliesOverflowDampingAndCapAfterTheRevealLimit() {
        val visualOffset = calculateSwipeVisualOffset(
            rawOffset = 400f,
            minOffset = -96f,
            maxOffset = 96f
        )

        assertTrue(visualOffset > 96f)
        assertTrue(visualOffset <= 96f * 1.35f)
    }

    @Test
    fun visualOffset_blocksUnavailableDirections() {
        val visualOffset = calculateSwipeVisualOffset(
            rawOffset = -100f,
            minOffset = 0f,
            maxOffset = 96f
        )

        assertEquals(0f, visualOffset, 0.001f)
    }

    @Test
    fun rawOffset_doesNotAccumulateInAnUnavailableDirection() {
        val blocked = accumulateSwipeRawOffset(
            currentRawOffset = 0f,
            dragAmount = -100f,
            minOffset = 0f,
            maxOffset = 96f,
        )
        val reversed = accumulateSwipeRawOffset(
            currentRawOffset = blocked,
            dragAmount = 20f,
            minOffset = 0f,
            maxOffset = 96f,
        )

        assertEquals(0f, blocked, 0.001f)
        assertEquals(20f, reversed, 0.001f)
    }

    @Test
    fun progress_isClampedToTheTriggerThreshold() {
        assertEquals(0f, calculateSwipeProgress(offset = 0f, triggerThreshold = 72f), 0.001f)
        assertEquals(0.5f, calculateSwipeProgress(offset = -36f, triggerThreshold = 72f), 0.001f)
        assertEquals(1f, calculateSwipeProgress(offset = 120f, triggerThreshold = 72f), 0.001f)
        assertEquals(0f, calculateSwipeProgress(offset = 120f, triggerThreshold = 0f), 0.001f)
    }
}
