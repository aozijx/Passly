package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationFreshnessPolicyTest {

    @Test
    fun highSensitivityRevealAlwaysRequiresFreshAuthentication() {
        assertTrue(
            authenticationRequiresFresh(
                AuthenticationPurpose.REVEAL_HIGH_SENSITIVITY_SECRET,
                reauthenticateSensitiveCopies = false
            )
        )
    }

    @Test
    fun copyAuthenticationFollowsGlobalCopySetting() {
        assertTrue(
            authenticationRequiresFresh(
                AuthenticationPurpose.COPY_SECRET,
                reauthenticateSensitiveCopies = true
            )
        )
        assertFalse(
            authenticationRequiresFresh(
                AuthenticationPurpose.COPY_SECRET,
                reauthenticateSensitiveCopies = false
            )
        )
    }

    @Test
    fun standardRevealReusesUnlockedSession() {
        assertFalse(
            authenticationRequiresFresh(
                AuthenticationPurpose.REVEAL_SECRET,
                reauthenticateSensitiveCopies = true
            )
        )
    }
}
