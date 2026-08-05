package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.authentication.AuthenticationMethodPolicy
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
            AuthenticationMethodPolicy.requiresFreshAuthentication(
                AuthenticationPurpose.REVEAL_HIGH_SENSITIVITY_SECRET,
                reauthenticateSensitiveCopies = false
            )
        )
    }

    @Test
    fun copyAuthenticationFollowsGlobalCopySetting() {
        assertTrue(
            AuthenticationMethodPolicy.requiresFreshAuthentication(
                AuthenticationPurpose.COPY_SECRET,
                reauthenticateSensitiveCopies = true
            )
        )
        assertFalse(
            AuthenticationMethodPolicy.requiresFreshAuthentication(
                AuthenticationPurpose.COPY_SECRET,
                reauthenticateSensitiveCopies = false
            )
        )
    }

    @Test
    fun standardRevealReusesUnlockedSession() {
        assertFalse(
            AuthenticationMethodPolicy.requiresFreshAuthentication(
                AuthenticationPurpose.REVEAL_SECRET,
                reauthenticateSensitiveCopies = true
            )
        )
    }

    @Test
    fun everydayPurposesRejectRecoveryCode() {
        val primaryMethods = AuthenticationMethodPolicy.PRIMARY_METHODS
        assertEquals(
            primaryMethods,
            AuthenticationMethodPolicy.allowedAuthenticationMethods(AuthenticationPurpose.UNLOCK_VAULT)
        )
        assertEquals(
            primaryMethods,
            AuthenticationMethodPolicy.allowedAuthenticationMethods(AuthenticationPurpose.BACKUP_EXPORT)
        )
        assertEquals(
            primaryMethods,
            AuthenticationMethodPolicy.allowedAuthenticationMethods(AuthenticationPurpose.CLEAR_DATABASE)
        )
        assertEquals(
            primaryMethods,
            AuthenticationMethodPolicy.allowedAuthenticationMethods(AuthenticationPurpose.REVEAL_SECRET)
        )
        assertEquals(
            primaryMethods,
            AuthenticationMethodPolicy.allowedAuthenticationMethods(AuthenticationPurpose.COPY_SECRET)
        )
    }

    @Test
    fun recoveryPurposesAcceptOnlyRecoveryCode() {
        val recoveryOnly = setOf(AuthenticationMethod.RECOVERY_CODE)
        assertEquals(
            recoveryOnly,
            AuthenticationMethodPolicy.allowedAuthenticationMethods(AuthenticationPurpose.RECOVERY_EXPORT)
        )
        assertEquals(
            recoveryOnly,
            AuthenticationMethodPolicy.allowedAuthenticationMethods(AuthenticationPurpose.RECOVER_AUTH_METHODS)
        )
    }

    @Test
    fun damagedDatabaseRecoveryAcceptsEveryEnvelopeMethod() {
        assertEquals(
            AuthenticationMethod.entries.toSet(),
            AuthenticationMethodPolicy.allowedAuthenticationMethods(AuthenticationPurpose.RECOVER_DATABASE)
        )
    }

    // ==================== 恢复码不作为日常替代方式 ====================

    @Test
    fun primaryMethodsExcludeRecoveryCode() {
        assertFalse(
            "PRIMARY_METHODS must not contain RECOVERY_CODE",
            AuthenticationMethod.RECOVERY_CODE in AuthenticationMethodPolicy.PRIMARY_METHODS
        )
        assertTrue(
            "PRIMARY_METHODS must contain APP_PASSWORD",
            AuthenticationMethod.APP_PASSWORD in AuthenticationMethodPolicy.PRIMARY_METHODS
        )
        assertTrue(
            "PRIMARY_METHODS must contain BIOMETRIC",
            AuthenticationMethod.BIOMETRIC in AuthenticationMethodPolicy.PRIMARY_METHODS
        )
    }

    @Test
    fun autofillRejectsRecoveryCode() {
        assertEquals(
            AuthenticationMethodPolicy.PRIMARY_METHODS,
            AuthenticationMethodPolicy.allowedAuthenticationMethods(AuthenticationPurpose.AUTOFILL)
        )
    }

    // ==================== 恢复模式边界 ====================

    @Test
    fun recoveryModePurposesAreExplicitlyScoped() {
        val recoveryPurposes = AuthenticationMethodPolicy.RECOVERY_MODE_PURPOSES
        assertEquals(
            "Recovery mode must allow exactly 2 purposes",
            2,
            recoveryPurposes.size
        )
        assertTrue(AuthenticationPurpose.RECOVERY_EXPORT in recoveryPurposes)
        assertTrue(AuthenticationPurpose.RECOVER_AUTH_METHODS in recoveryPurposes)
        assertFalse(
            "UNLOCK_VAULT must not be in recovery mode purposes",
            AuthenticationPurpose.UNLOCK_VAULT in recoveryPurposes
        )
        assertFalse(
            "BACKUP_EXPORT must not be in recovery mode purposes",
            AuthenticationPurpose.BACKUP_EXPORT in recoveryPurposes
        )
    }

    @Test
    fun recoveryModeReusablePurposesMatchRecoveryPurposes() {
        assertEquals(
            "RECOVERY_MODE_REUSABLE_PURPOSES must match RECOVERY_MODE_PURPOSES",
            AuthenticationMethodPolicy.RECOVERY_MODE_PURPOSES,
            AuthenticationMethodPolicy.RECOVERY_MODE_REUSABLE_PURPOSES
        )
    }

    // ==================== 恢复码导出 ====================

    @Test
    fun recoveryExportRequiresOnlyRecoveryCode() {
        val methods = AuthenticationMethodPolicy.allowedAuthenticationMethods(
            AuthenticationPurpose.RECOVERY_EXPORT
        )
        assertEquals(
            "RECOVERY_EXPORT must allow only RECOVERY_CODE",
            setOf(AuthenticationMethod.RECOVERY_CODE),
            methods
        )
        assertFalse(
            "APP_PASSWORD must not be allowed for RECOVERY_EXPORT",
            AuthenticationMethod.APP_PASSWORD in methods
        )
        assertFalse(
            "BIOMETRIC must not be allowed for RECOVERY_EXPORT",
            AuthenticationMethod.BIOMETRIC in methods
        )
    }

    @Test
    fun normalBackupExportRejectsRecoveryCode() {
        val methods = AuthenticationMethodPolicy.allowedAuthenticationMethods(
            AuthenticationPurpose.BACKUP_EXPORT
        )
        assertFalse(
            "RECOVERY_CODE must not be allowed for BACKUP_EXPORT",
            AuthenticationMethod.RECOVERY_CODE in methods
        )
        assertTrue(
            "APP_PASSWORD must be allowed for BACKUP_EXPORT",
            AuthenticationMethod.APP_PASSWORD in methods
        )
    }

    // ==================== 最后认证方式判断 ====================

    @Test
    fun manageAppPasswordRejectsRecoveryCode() {
        val methods = AuthenticationMethodPolicy.allowedAuthenticationMethods(
            AuthenticationPurpose.MANAGE_APP_PASSWORD
        )
        assertEquals(
            "MANAGE_APP_PASSWORD must use PRIMARY_METHODS only",
            AuthenticationMethodPolicy.PRIMARY_METHODS,
            methods
        )
        assertFalse(
            "RECOVERY_CODE must not be allowed for MANAGE_APP_PASSWORD",
            AuthenticationMethod.RECOVERY_CODE in methods
        )
    }

    @Test
    fun changeBiometricPolicyRejectsRecoveryCode() {
        val methods = AuthenticationMethodPolicy.allowedAuthenticationMethods(
            AuthenticationPurpose.CHANGE_BIOMETRIC_POLICY
        )
        assertEquals(
            "CHANGE_BIOMETRIC_POLICY must use PRIMARY_METHODS only",
            AuthenticationMethodPolicy.PRIMARY_METHODS,
            methods
        )
        assertFalse(
            "RECOVERY_CODE must not be allowed for CHANGE_BIOMETRIC_POLICY",
            AuthenticationMethod.RECOVERY_CODE in methods
        )
    }

    // ==================== 恢复认证方式 ====================

    @Test
    fun recoverAuthMethodsRequiresOnlyRecoveryCode() {
        val methods = AuthenticationMethodPolicy.allowedAuthenticationMethods(
            AuthenticationPurpose.RECOVER_AUTH_METHODS
        )
        assertEquals(
            "RECOVER_AUTH_METHODS must allow only RECOVERY_CODE",
            setOf(AuthenticationMethod.RECOVERY_CODE),
            methods
        )
    }

    @Test
    fun recoverAuthMethodsRequiresFreshAuthentication() {
        assertTrue(
            AuthenticationMethodPolicy.requiresFreshAuthentication(
                AuthenticationPurpose.RECOVER_AUTH_METHODS,
                reauthenticateSensitiveCopies = false
            )
        )
    }

    @Test
    fun recoveryExportRequiresFreshAuthentication() {
        assertTrue(
            AuthenticationMethodPolicy.requiresFreshAuthentication(
                AuthenticationPurpose.RECOVERY_EXPORT,
                reauthenticateSensitiveCopies = false
            )
        )
    }
}
