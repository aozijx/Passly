package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationMethod
import org.junit.Assert.assertEquals
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

    @Test
    fun everydayPurposesRejectRecoveryCode() {
        val primaryMethods = setOf(
            AuthenticationMethod.BIOMETRIC,
            AuthenticationMethod.APP_PASSWORD
        )
        assertEquals(
            primaryMethods,
            allowedAuthenticationMethods(AuthenticationPurpose.UNLOCK_VAULT)
        )
        assertEquals(
            primaryMethods,
            allowedAuthenticationMethods(AuthenticationPurpose.BACKUP_EXPORT)
        )
        assertEquals(
            primaryMethods,
            allowedAuthenticationMethods(AuthenticationPurpose.CLEAR_DATABASE)
        )
        assertEquals(
            primaryMethods,
            allowedAuthenticationMethods(AuthenticationPurpose.REVEAL_SECRET)
        )
        assertEquals(
            primaryMethods,
            allowedAuthenticationMethods(AuthenticationPurpose.COPY_SECRET)
        )
    }

    @Test
    fun recoveryPurposesAcceptOnlyRecoveryCode() {
        val recoveryOnly = setOf(AuthenticationMethod.RECOVERY_CODE)
        assertEquals(
            recoveryOnly,
            allowedAuthenticationMethods(AuthenticationPurpose.RECOVERY_EXPORT)
        )
        assertEquals(
            recoveryOnly,
            allowedAuthenticationMethods(AuthenticationPurpose.RECOVER_AUTH_METHODS)
        )
    }

    @Test
    fun damagedDatabaseRecoveryAcceptsEveryEnvelopeMethod() {
        assertEquals(
            AuthenticationMethod.entries.toSet(),
            allowedAuthenticationMethods(AuthenticationPurpose.RECOVER_DATABASE)
        )
    }
}
