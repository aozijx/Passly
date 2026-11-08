package com.aozijx.passly.domain.settings.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppCornerRadiusConstraintsTest {
    @Test
    fun `application corner radius defines and normalizes the supported range`() {
        assertEquals(0f, AppCornerRadiusConstraints.MIN_DP)
        assertEquals(48f, AppCornerRadiusConstraints.MAX_DP)
        assertEquals(16f, AppCornerRadiusConstraints.DEFAULT_DP)
        assertEquals(0f, AppCornerRadiusConstraints.normalize(-1f))
        assertEquals(24f, AppCornerRadiusConstraints.normalize(24f))
        assertEquals(48f, AppCornerRadiusConstraints.normalize(49f))
    }
}
