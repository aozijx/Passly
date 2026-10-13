package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.LockReason
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationSessionReusePolicyTest {

    @Test
    fun `locking session is never reused even before storage finishes sealing`() {
        assertFalse(
            canReuseSession(
                state = AuthenticationState.Locking(LockReason.AUTOFILL_REQUEST_FINISHED),
                storageUnlocked = true,
                requiresFreshAuthentication = false,
                purpose = AuthenticationPurpose.AUTOFILL,
            ),
        )
    }

    @Test
    fun `authenticated unlocked session remains reusable when freshness is not required`() {
        assertTrue(
            canReuseSession(
                state = AuthenticationState.Authenticated(authenticatedAtMs = 1L),
                storageUnlocked = true,
                requiresFreshAuthentication = false,
                purpose = AuthenticationPurpose.UNLOCK_VAULT,
            ),
        )
    }

    @Test
    fun `autofill session is reusable only by autofill`() {
        val state = AuthenticationState.AutofillAuthenticated(authenticatedAtMs = 1L)

        assertTrue(
            canReuseSession(
                state = state,
                storageUnlocked = true,
                requiresFreshAuthentication = false,
                purpose = AuthenticationPurpose.AUTOFILL,
            ),
        )
        assertFalse(
            canReuseSession(
                state = state,
                storageUnlocked = true,
                requiresFreshAuthentication = false,
                purpose = AuthenticationPurpose.UNLOCK_VAULT,
            ),
        )
    }
}
