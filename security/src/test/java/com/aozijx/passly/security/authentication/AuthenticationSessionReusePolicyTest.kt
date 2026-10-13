package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationSessionReusePolicyTest {

    @Test
    fun `locking session is never reused even before storage finishes sealing`() {
        assertFalse(
            canReuseFullSession(
                state = AuthenticationState.Locking(LockReason.AUTOFILL_REQUEST_FINISHED),
                storageUnlocked = true,
                requiresFreshAuthentication = false,
            ),
        )
    }

    @Test
    fun `authenticated unlocked session remains reusable when freshness is not required`() {
        assertTrue(
            canReuseFullSession(
                state = AuthenticationState.Authenticated(authenticatedAtMs = 1L),
                storageUnlocked = true,
                requiresFreshAuthentication = false,
            ),
        )
    }
}
