package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.policy.AuthenticationMethodPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationMethodPolicyTest {
    @Test
    fun vaultUnlockAndAutofillEstablishHighSensitivityKeyWindow() {
        listOf(AuthenticationPurpose.UNLOCK_VAULT, AuthenticationPurpose.AUTOFILL).forEach { purpose ->
            assertTrue(
                "$purpose must unlock the sensitive data key",
                AuthenticationMethodPolicy.unlocksSensitiveDataKey(purpose),
            )
        }
    }

    @Test
    fun highSensitivityRevealAlwaysRequiresFreshAuthentication() {
        assertTrue(
            AuthenticationMethodPolicy.requiresFreshAuthentication(
                AuthenticationPurpose.REVEAL_HIGH_SENSITIVITY_SECRET,
                reauthenticateSensitiveCopies = false,
            ),
        )
    }

    @Test
    fun copyAuthenticationFollowsGlobalCopySetting() {
        assertTrue(
            AuthenticationMethodPolicy.requiresFreshAuthentication(
                AuthenticationPurpose.COPY_SECRET,
                reauthenticateSensitiveCopies = true,
            ),
        )
        assertFalse(
            AuthenticationMethodPolicy.requiresFreshAuthentication(
                AuthenticationPurpose.COPY_SECRET,
                reauthenticateSensitiveCopies = false,
            ),
        )
    }

    @Test
    fun standardRevealReusesUnlockedSession() {
        assertFalse(
            AuthenticationMethodPolicy.requiresFreshAuthentication(
                AuthenticationPurpose.REVEAL_SECRET,
                reauthenticateSensitiveCopies = true,
            ),
        )
    }

    @Test
    fun allowedMethodsReserveRecoveryCodeForAuthenticationRecovery() {
        AuthenticationPurpose.entries
            .filter { it != AuthenticationPurpose.RECOVER_AUTH_METHODS }
            .forEach { purpose ->
                assertEquals(
                    "$purpose must use primary methods",
                    AuthenticationMethodPolicy.PRIMARY_METHODS,
                    AuthenticationMethodPolicy.allowedAuthenticationMethods(purpose),
                )
            }
        assertEquals(
            setOf(AuthenticationMethod.RECOVERY_CODE),
            AuthenticationMethodPolicy.allowedAuthenticationMethods(AuthenticationPurpose.RECOVER_AUTH_METHODS),
        )
    }

    @Test
    fun primaryMethodsExcludeRecoveryCode() {
        assertFalse(
            "PRIMARY_METHODS must not contain RECOVERY_CODE",
            AuthenticationMethod.RECOVERY_CODE in AuthenticationMethodPolicy.PRIMARY_METHODS,
        )
        assertTrue(
            "PRIMARY_METHODS must contain APP_PASSWORD",
            AuthenticationMethod.APP_PASSWORD in AuthenticationMethodPolicy.PRIMARY_METHODS,
        )
        assertTrue(
            "PRIMARY_METHODS must contain BIOMETRIC",
            AuthenticationMethod.BIOMETRIC in AuthenticationMethodPolicy.PRIMARY_METHODS,
        )
    }

    @Test
    fun recoveryModePurposesAreExplicitlyScoped() {
        val recoveryPurposes = AuthenticationMethodPolicy.RECOVERY_MODE_PURPOSES
        assertEquals(
            "Recovery mode must allow only credential rebuilding",
            1,
            recoveryPurposes.size,
        )
        assertTrue(AuthenticationPurpose.RECOVER_AUTH_METHODS in recoveryPurposes)
        assertFalse(
            "UNLOCK_VAULT must not be in recovery mode purposes",
            AuthenticationPurpose.UNLOCK_VAULT in recoveryPurposes,
        )
        assertFalse(
            "BACKUP_EXPORT must not be in recovery mode purposes",
            AuthenticationPurpose.BACKUP_EXPORT in recoveryPurposes,
        )
    }

    @Test
    fun recoveryModeReusablePurposesMatchRecoveryPurposes() {
        assertEquals(
            "RECOVERY_MODE_REUSABLE_PURPOSES must match RECOVERY_MODE_PURPOSES",
            AuthenticationMethodPolicy.RECOVERY_MODE_PURPOSES,
            AuthenticationMethodPolicy.RECOVERY_MODE_REUSABLE_PURPOSES,
        )
    }

    @Test
    fun recoverAuthMethodsRequiresFreshAuthentication() {
        assertTrue(
            AuthenticationMethodPolicy.requiresFreshAuthentication(
                AuthenticationPurpose.RECOVER_AUTH_METHODS,
                reauthenticateSensitiveCopies = false,
            ),
        )
    }
}
