package com.aozijx.passly.domain.authentication

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPasswordPolicyTest {
    @Test
    fun rejectsPasswordsShorterThanSixCharacters() {
        assertFalse(AppPasswordPolicy.acceptsLength(0))
        assertFalse(AppPasswordPolicy.acceptsLength(AppPasswordPolicy.MIN_LENGTH - 1))
    }

    @Test
    fun acceptsPasswordsAtOrAboveMinimumLength() {
        assertTrue(AppPasswordPolicy.acceptsLength(AppPasswordPolicy.MIN_LENGTH))
        assertTrue(AppPasswordPolicy.acceptsLength(128))
    }
}
