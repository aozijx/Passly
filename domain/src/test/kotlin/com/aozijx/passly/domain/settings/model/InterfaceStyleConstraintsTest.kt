package com.aozijx.passly.domain.settings.model

import org.junit.Assert.assertEquals
import org.junit.Test

class InterfaceStyleConstraintsTest {

    @Test
    fun `rounded group defaults use 16dp outside and 6dp inside`() {
        assertEquals(16f, InterfaceStyleConstraints.DEFAULT_OUTER_RADIUS_DP)
        assertEquals(6f, InterfaceStyleConstraints.DEFAULT_INNER_RADIUS_DP)
    }
}
