package com.aozijx.passly.domain.settings.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppCornerRadiusConstraintsTest {

    @Test
    fun `application corner radius keeps the supported range and default`() {
        assertEquals(0f, AppCornerRadiusConstraints.MIN_DP)
        assertEquals(48f, AppCornerRadiusConstraints.MAX_DP)
        assertEquals(16f, AppCornerRadiusConstraints.DEFAULT_DP)
    }
}
