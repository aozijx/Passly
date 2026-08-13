package com.aozijx.passly.domain.access.model

import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.domain.access.policy.AppPasswordViolation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthenticationTest {
    @Test
    fun `authentication request requires at least one method`() {
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationRequest(
                purpose = AuthenticationPurpose.UNLOCK_VAULT,
                allowedMethods = emptySet(),
            )
        }
    }

    @Test
    fun `attempt status rejects impossible counts`() {
        assertThrows(IllegalArgumentException::class.java) {
            AttemptStatus(used = 4, limit = 3)
        }
    }

    @Test
    fun `password policy reports all violated rules`() {
        assertEquals(
            setOf(
                AppPasswordViolation.TOO_SHORT,
                AppPasswordViolation.INSUFFICIENT_CHARACTER_VARIETY,
            ),
            AppPasswordPolicy.DEFAULT.validate("111".toCharArray()),
        )
    }
}
