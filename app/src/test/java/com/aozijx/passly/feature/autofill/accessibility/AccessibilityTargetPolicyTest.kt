package com.aozijx.passly.feature.autofill.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityTargetPolicyTest {
    @Test
    fun acceptsOnlyOriginalWindowAndPackageWithinThirtySeconds() {
        assertTrue(AccessibilityTargetPolicy.canFill("com.example.app", 7, 1_000, "com.example.app", 7, 30_999))
        assertFalse(AccessibilityTargetPolicy.canFill("com.example.app", 7, 1_000, "com.other", 7, 2_000))
        assertFalse(AccessibilityTargetPolicy.canFill("com.example.app", 7, 1_000, "com.example.app", 8, 2_000))
        assertFalse(AccessibilityTargetPolicy.canFill("com.example.app", 7, 1_000, "com.example.app", 7, 31_001))
    }
}
