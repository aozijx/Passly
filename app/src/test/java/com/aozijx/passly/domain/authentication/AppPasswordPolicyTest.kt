package com.aozijx.passly.domain.authentication

import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
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
}
