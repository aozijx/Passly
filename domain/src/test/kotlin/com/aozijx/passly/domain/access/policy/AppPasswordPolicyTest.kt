package com.aozijx.passly.domain.access.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPasswordPolicyTest {
    @Test
    fun rejectsPasswordsShorterThanSixCharacters() {
        assertFalse(AppPasswordPolicy.DEFAULT.acceptsLength(0))
        assertFalse(AppPasswordPolicy.DEFAULT.acceptsLength(AppPasswordPolicy.DEFAULT.minimumLength - 1))
    }

    @Test
    fun acceptsPasswordsAtOrAboveMinimumLength() {
        assertTrue(AppPasswordPolicy.DEFAULT.acceptsLength(AppPasswordPolicy.DEFAULT.minimumLength))
        assertTrue(AppPasswordPolicy.DEFAULT.acceptsLength(128))
    }

    @Test
    fun `validation reports every violated rule`() {
        assertEquals(
            setOf(
                AppPasswordViolation.TOO_SHORT,
                AppPasswordViolation.INSUFFICIENT_CHARACTER_VARIETY,
            ),
            AppPasswordPolicy.DEFAULT.validate("111".toCharArray()),
        )
    }
}
