package com.aozijx.passly.feature.settings.security

import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.BiometricConfiguration
import com.aozijx.passly.domain.access.model.CancellationReason
import com.aozijx.passly.domain.access.port.AuthenticationMethodAvailability
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.access.port.BiometricConfigurationSource
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityAuthenticationSettingsInteractorTest {
    @Test
    fun `policy change follows the committed binding without a second preference write`() = runTest {
        val provisioner = FakeProvisioner()
        val interactor = interactor(provisioner = provisioner)
        assertEquals(
            AuthenticationResult.Success(AuthenticationMethod.BIOMETRIC),
            interactor.setKeyInvalidationPolicy(enabled = false),
        )
        assertEquals(BiometricConfiguration.Configured(false), interactor.biometricConfiguration.first())
    }

    @Test
    fun `enabling uses committed policy or secure first-key policy`() = runTest {
        for (configuration in listOf(
            BiometricConfiguration.NotConfigured,
            BiometricConfiguration.Configured(false),
            BiometricConfiguration.Configured(true),
        )) {
            val provisioner = FakeProvisioner()
            provisioner.configuration.value = configuration
            val interactor = interactor(provisioner = provisioner)
            interactor.setBiometricEnabled(enabled = true)
            val expected = configuration != BiometricConfiguration.Configured(false)
            assertEquals(listOf(expected), provisioner.requestedPolicies)
        }
    }

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
        val restricted = AuthenticationResult.Failure(AuthenticationFailure(AuthenticationFailureCode.SESSION_MODE_RESTRICTED))
        assertEquals(restricted, interactor.setBiometricEnabled(enabled = true))
        assertEquals(restricted, interactor.setKeyInvalidationPolicy(enabled = false))
        assertEquals(0, provisioner.biometricChanges)
    }

    @Test
    fun `cancelled rotation leaves committed configuration unchanged`() = runTest {
        val provisioner = FakeProvisioner(rotateResult = AuthenticationResult.Cancelled(CancellationReason.USER))
        provisioner.configuration.value = BiometricConfiguration.Configured(true)
        val interactor = interactor(provisioner = provisioner)
        assertEquals(AuthenticationResult.Cancelled(CancellationReason.USER), interactor.setKeyInvalidationPolicy(false))
        assertEquals(BiometricConfiguration.Configured(true), interactor.biometricConfiguration.first())
        provisioner.rotateResult = AuthenticationResult.Success(AuthenticationMethod.BIOMETRIC)
        assertEquals(AuthenticationResult.Success(AuthenticationMethod.BIOMETRIC), interactor.setKeyInvalidationPolicy(false))
        assertEquals(BiometricConfiguration.Configured(false), interactor.biometricConfiguration.first())
    }

    private fun interactor(
        methods: MutableStateFlow<AuthenticationMethods> = MutableStateFlow(AuthenticationMethods()),
        provisioner: FakeProvisioner = FakeProvisioner(),
        state: AuthenticationState = AuthenticationState.Authenticated(authenticatedAtMs = 1),
    ): SecurityAuthenticationSettingsInteractor {
        val authenticationState = MutableStateFlow(state)
        return SecurityAuthenticationSettingsInteractor(
            authenticationMethodAvailability = object : AuthenticationMethodAvailability { override val methods = methods },
            secureSessionAccessState = object : SecureSessionAccessState {
                override val authenticationState = authenticationState
                override fun isUnlocked() = authenticationState.value is AuthenticationState.Authenticated ||
                    authenticationState.value is AuthenticationState.RecoveryMode
            },
            authenticationMethodProvisioner = provisioner,
            configurationSource = object : BiometricConfigurationSource {
                override val biometricConfiguration = provisioner.configuration
            },
        )
    }

    private class FakeProvisioner(
        var rotateResult: AuthenticationResult = AuthenticationResult.Success(AuthenticationMethod.BIOMETRIC),
    ) : AuthenticationMethodProvisioner {
        val configuration = MutableStateFlow<BiometricConfiguration>(BiometricConfiguration.NotConfigured)
        val requestedPolicies = mutableListOf<Boolean>()
        var biometricChanges = 0
        override suspend fun authorizeAppPasswordManagement() = error("Not used")
        override suspend fun setAppPassword(password: CharArray) = error("Not used")
        override suspend fun changeAppPassword(currentPassword: CharArray, newPassword: CharArray) = error("Not used")
        override suspend fun disableAppPassword() = error("Not used")
        override suspend fun disableBiometric(): AuthenticationResult {
            biometricChanges++
            if (rotateResult is AuthenticationResult.Success) configuration.value = BiometricConfiguration.NotConfigured
            return rotateResult
        }
        override suspend fun rotateBiometricPolicy(invalidateOnEnrollment: Boolean): AuthenticationResult {
            biometricChanges++
            requestedPolicies += invalidateOnEnrollment
            if (rotateResult is AuthenticationResult.Success) {
                configuration.value = BiometricConfiguration.Configured(invalidateOnEnrollment)
            }
            return rotateResult
        }
        override suspend fun hasRecoveryCode() = true
        override suspend fun checkRecoveryCode(code: CharArray) = true
    }
}
