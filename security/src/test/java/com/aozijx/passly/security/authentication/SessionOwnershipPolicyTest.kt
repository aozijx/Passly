package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionOwnershipPolicyTest {

    @Test
    fun `autofill authentication publishes only temporary access`() {
        assertEquals(
            AuthenticationState.AutofillAuthenticated(authenticatedAtMs = 7L),
            authenticatedStateFor(AuthenticationPurpose.AUTOFILL, authenticatedAtMs = 7L),
        )
    }

    @Test
    fun `vault authentication publishes full app access`() {
        assertEquals(
            AuthenticationState.Authenticated(authenticatedAtMs = 7L),
            authenticatedStateFor(AuthenticationPurpose.UNLOCK_VAULT, authenticatedAtMs = 7L),
        )
    }

    @Test
    fun `autofill release cannot seal a promoted app session`() {
        assertFalse(
            shouldApplyLock(
                state = AuthenticationState.Authenticated(authenticatedAtMs = 7L),
                reason = LockReason.AUTOFILL_REQUEST_FINISHED,
            ),
        )
        assertTrue(
            shouldApplyLock(
                state = AuthenticationState.AutofillAuthenticated(authenticatedAtMs = 7L),
                reason = LockReason.AUTOFILL_REQUEST_FINISHED,
            ),
        )
    }
}
