package com.aozijx.passly.presentation.ui.shared.components.group

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemShapes
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SegmentedSettingsShapePolicyTest {

    @Test
    fun `stable selection keeps the group outline and preserves press feedback`() {
        val shapes = ListItemShapes(
            shape = RoundedCornerShape(16.dp),
            selectedShape = RoundedCornerShape(28.dp),
            pressedShape = RoundedCornerShape(8.dp),
            focusedShape = RoundedCornerShape(12.dp),
            hoveredShape = RoundedCornerShape(14.dp),
            draggedShape = RoundedCornerShape(20.dp),
        )

        val stableShapes = shapes.withStableSelectionShape()

        assertEquals(stableShapes.shape, stableShapes.selectedShape)
        assertEquals(shapes.pressedShape, stableShapes.pressedShape)
        assertNotEquals(stableShapes.shape, stableShapes.pressedShape)
    }
}
