package com.aozijx.passly.feature.settings.security

import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.CancellationReason
import com.aozijx.passly.domain.access.port.AuthenticationMethodAvailability
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.settings.model.SecuritySettings
import com.aozijx.passly.domain.settings.port.SecuritySettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityAuthenticationSettingsInteractorTest {
    @Test
    fun `biometric availability is exposed as a semantic setting`() = runTest {
        val methods = MutableStateFlow(AuthenticationMethods())
        val interactor = interactor(methods = methods)

        assertFalse(interactor.isBiometricEnabled.first())

        methods.value = AuthenticationMethods(setOf(AuthenticationMethod.BIOMETRIC))
        assertTrue(interactor.isBiometricEnabled.first())
    }

    @Test
    fun `recovery mode rejects authentication changes and wipes recovery code`() = runTest {
        val provisioner = FakeProvisioner()
        val code = charArrayOf('1', '2', '3')
        val interactor = interactor(
            provisioner = provisioner,
            state = AuthenticationState.RecoveryMode(authenticatedAtMs = 1),
        )

        assertFalse(interactor.verifyRecoveryCode(code))
        assertTrue(code.all { it == '\u0000' })
        assertEquals(
            SecurityAuthenticationChangeResult.NOT_APPLIED,
            interactor.setBiometricEnabled(enabled = true, invalidateOnEnrollment = true),
        )
        assertEquals(
            SecurityAuthenticationChangeResult.NOT_APPLIED,
            interactor.setKeyInvalidationPolicy(enabled = false),
        )
        assertEquals(0, provisioner.biometricChanges)
    }

    @Test
    fun `key invalidation setting persists only after successful biometric rotation`() = runTest {
        val repository = FakeSecuritySettingsRepository()
        val provisioner = FakeProvisioner(
            rotateResult = AuthenticationResult.Cancelled(CancellationReason.USER),
        )
        val interactor = interactor(provisioner = provisioner, repository = repository)

        assertEquals(
            SecurityAuthenticationChangeResult.NOT_APPLIED,
            interactor.setKeyInvalidationPolicy(enabled = false),
        )
        assertEquals(emptyList<Boolean>(), repository.invalidationPolicies)

        provisioner.rotateResult = AuthenticationResult.Success(AuthenticationMethod.BIOMETRIC)
        assertEquals(
            SecurityAuthenticationChangeResult.APPLIED,
            interactor.setKeyInvalidationPolicy(enabled = false),
        )
        assertEquals(listOf(false), repository.invalidationPolicies)
    }

    private fun interactor(
        methods: MutableStateFlow<AuthenticationMethods> = MutableStateFlow(AuthenticationMethods()),
        provisioner: FakeProvisioner = FakeProvisioner(),
        repository: FakeSecuritySettingsRepository = FakeSecuritySettingsRepository(),
        state: AuthenticationState = AuthenticationState.Authenticated(authenticatedAtMs = 1),
    ): SecurityAuthenticationSettingsInteractor {
        val authenticationState = MutableStateFlow(state)
        return SecurityAuthenticationSettingsInteractor(
            authenticationMethodAvailability = object : AuthenticationMethodAvailability {
                override val methods = methods
            },
            secureSessionAccessState = object : SecureSessionAccessState {
                override val authenticationState = authenticationState
                override fun isUnlocked() =
                    authenticationState.value is AuthenticationState.Authenticated ||
                        authenticationState.value is AuthenticationState.RecoveryMode
            },
            authenticationMethodProvisioner = provisioner,
            securitySettingsRepository = repository,
        )
    }

    private class FakeProvisioner(
        var rotateResult: AuthenticationResult =
            AuthenticationResult.Success(AuthenticationMethod.BIOMETRIC),
    ) : AuthenticationMethodProvisioner {
        var biometricChanges = 0

        override suspend fun authorizeAppPasswordManagement() = error("Not used")
        override suspend fun setAppPassword(password: CharArray) = error("Not used")
        override suspend fun changeAppPassword(
            currentPassword: CharArray,
            newPassword: CharArray,
        ) = error("Not used")
        override suspend fun disableAppPassword() = error("Not used")
        override suspend fun disableBiometric(): AuthenticationResult {
            biometricChanges += 1
            return rotateResult
        }
        override suspend fun rotateBiometricPolicy(invalidateOnEnrollment: Boolean): AuthenticationResult {
            biometricChanges += 1
            return rotateResult
        }
        override suspend fun hasRecoveryCode() = true
        override suspend fun checkRecoveryCode(code: CharArray) = true
    }

    private class FakeSecuritySettingsRepository : SecuritySettingsRepository {
        override val security = MutableStateFlow(SecuritySettings())
        val invalidationPolicies = mutableListOf<Boolean>()

        override suspend fun setInvalidateBiometricKeyOnChange(enabled: Boolean) {
            invalidationPolicies += enabled
        }

        override suspend fun setSecureContentEnabled(enabled: Boolean) = Unit
        override suspend fun setFlipToLockEnabled(enabled: Boolean) = Unit
        override suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean) = Unit
        override suspend fun setLockOnBackground(enabled: Boolean) = Unit
        override suspend fun setLockTimeout(timeoutMs: Long) = Unit
        override suspend fun setReauthenticateSensitiveCopies(enabled: Boolean) = Unit
        override suspend fun setClipboardClearEnabled(enabled: Boolean) = Unit
        override suspend fun setClipboardClearDelaySeconds(delaySeconds: Int) = Unit
    }
}
