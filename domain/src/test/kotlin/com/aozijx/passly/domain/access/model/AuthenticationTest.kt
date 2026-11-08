package com.aozijx.passly.domain.access.model

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
}
