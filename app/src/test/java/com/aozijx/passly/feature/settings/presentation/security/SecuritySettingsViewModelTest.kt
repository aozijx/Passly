package com.aozijx.passly.feature.settings.presentation.security

import androidx.lifecycle.ViewModelStore
import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.BiometricConfiguration
import com.aozijx.passly.domain.access.port.BiometricConfigurationSource
import java.io.IOException
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
import com.aozijx.passly.feature.settings.security.SecurityAuthenticationSettingsInteractor
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SecuritySettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `authentication change reserves operation before dispatch and cancellation permits retry`() = runTest {
        val release = CompletableDeferred<Unit>()
        val fixture = Fixture {
            release.await()
            AuthenticationResult.Cancelled(CancellationReason.USER)
        }
        val model = fixture.model()
        backgroundScope.launch { model.uiState.collect {} }
        runCurrent()
        model.onAction(SecuritySettingsAction.SetBiometricEnabled(false))
        model.onAction(SecuritySettingsAction.SetInvalidateKeyOnBiometricChange(false))
        runCurrent()
        assertEquals(1, fixture.provisioner.calls)
        assertEquals(true, (model.uiState.value as SecuritySettingsUiState.Ready).isAuthenticationChangeInProgress)
        release.complete(Unit)
        runCurrent()
        assertEquals(false, (model.uiState.value as SecuritySettingsUiState.Ready).isAuthenticationChangeInProgress)
        model.onAction(SecuritySettingsAction.SetInvalidateKeyOnBiometricChange(false))
        runCurrent()
        assertEquals(2, fixture.provisioner.calls)
    }

    @Test
    fun `authentication cancellation is silent and failures retain last-method explanation`() = runTest {
        val results = listOf(
            AuthenticationResult.Cancelled(CancellationReason.USER),
            AuthenticationResult.Failure(AuthenticationFailure(AuthenticationFailureCode.LAST_METHOD_REQUIRED)),
        )
        for (result in results) {
            val fixture = Fixture { result }
            val model = fixture.model()
            val errors = mutableListOf<UiErrorMessage>()
            backgroundScope.launch { model.errors.toList(errors) }
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()
            model.onAction(SecuritySettingsAction.SetBiometricEnabled(false))
            runCurrent()
            val expected = if (result is AuthenticationResult.Cancelled) emptyList()
                else listOf(UiErrorMessage(R.string.settings_auth_last_method_required))
            assertEquals(expected, errors)
            assertEquals(true, (model.uiState.value as SecuritySettingsUiState.Ready).isBiometricEnabled)
        }
    }

    @Test
    fun `invalidated biometric key explains that authentication must be configured again`() = runTest {
        val fixture = Fixture {
            AuthenticationResult.Failure(
                AuthenticationFailure(AuthenticationFailureCode.KEY_INVALIDATED),
            )
        }
        val model = fixture.model()
        val errors = mutableListOf<UiErrorMessage>()
        backgroundScope.launch { model.errors.toList(errors) }
        backgroundScope.launch { model.uiState.collect {} }
        runCurrent()

        model.onAction(SecuritySettingsAction.SetInvalidateKeyOnBiometricChange(false))
        runCurrent()

        assertEquals(listOf(UiErrorMessage(R.string.notice_security_key_invalidated)), errors)
    }

    @Test
    fun `ordinary preference failures and authentication exceptions report localized errors and permit retry`() = runTest {
        val fixture = Fixture(save = { throw IOException("synthetic preference failure") }) {
            throw IOException("synthetic authentication failure")
        }
        val model = fixture.model()
        val errors = mutableListOf<UiErrorMessage>()
        backgroundScope.launch { model.errors.toList(errors) }
        backgroundScope.launch { model.uiState.collect {} }
        runCurrent()
        val before = model.uiState.value
        model.onAction(SecuritySettingsAction.SetLockTimeout(90_000L))
        model.onAction(SecuritySettingsAction.ToggleLockOnBackground(false))
        model.onAction(SecuritySettingsAction.SetBiometricEnabled(false))
        runCurrent()
        model.onAction(SecuritySettingsAction.SetInvalidateKeyOnBiometricChange(false))
        runCurrent()
        assertEquals(List(4) { UiErrorMessage(R.string.ui_error_file_io) }, errors)
        assertEquals(2, fixture.provisioner.calls)
        assertEquals(before, model.uiState.value)
    }

    @Test
    fun `hidden invalidation action cannot enable biometrics while disabled`() = runTest {
        val fixture = Fixture { AuthenticationResult.Success(AuthenticationMethod.BIOMETRIC) }
        fixture.methods.value = AuthenticationMethods()
        val model = fixture.model()
        backgroundScope.launch { model.uiState.collect {} }
        runCurrent()
        model.onAction(SecuritySettingsAction.SetInvalidateKeyOnBiometricChange(false))
        runCurrent()
        assertEquals(0, fixture.provisioner.calls)
    }

    @Test
    fun `committed configuration updates policy without another availability emission`() = runTest {
        val fixture = Fixture { AuthenticationResult.Success(AuthenticationMethod.BIOMETRIC) }
        val model = fixture.model()
        backgroundScope.launch { model.uiState.collect {} }
        runCurrent()
        fixture.configuration.value = BiometricConfiguration.Configured(false)
        runCurrent()
        assertEquals(false, (model.uiState.value as SecuritySettingsUiState.Ready).isInvalidateKeyOnBioChange)
        fixture.configuration.value = BiometricConfiguration.NotConfigured
        runCurrent()
        assertEquals(false, (model.uiState.value as SecuritySettingsUiState.Ready).isBiometricEnabled)
    }

    @Test
    fun `destroyed authentication change cannot emit late failure or run queued retry`() = runTest {
        val release = CompletableDeferred<Unit>()
        val fixture = Fixture {
            withContext(NonCancellable) { release.await() }
            AuthenticationResult.Failure(AuthenticationFailure(AuthenticationFailureCode.LAST_METHOD_REQUIRED))
        }
        val model = fixture.model()
        val errors = mutableListOf<UiErrorMessage>()
        backgroundScope.launch { model.errors.toList(errors) }
        backgroundScope.launch { model.uiState.collect {} }
        runCurrent()
        model.onAction(SecuritySettingsAction.SetBiometricEnabled(false))
        runCurrent()
        ViewModelStore().apply { put("security", model); clear() }
        release.complete(Unit)
        runCurrent()
        model.onAction(SecuritySettingsAction.SetBiometricEnabled(false))
        runCurrent()
        assertEquals(1, fixture.provisioner.calls)
        assertEquals(emptyList<UiErrorMessage>(), errors)
    }

    private class Fixture(save: suspend () -> Unit = {}, operation: suspend () -> AuthenticationResult) {
        val repository = Repository(save)
        val provisioner = Provisioner(operation)
        val methods = MutableStateFlow(AuthenticationMethods(setOf(AuthenticationMethod.BIOMETRIC)))
        val configuration = MutableStateFlow<BiometricConfiguration>(BiometricConfiguration.Configured(true))
        private val session = object : SecureSessionAccessState {
            override val authenticationState = MutableStateFlow<AuthenticationState>(
                AuthenticationState.Authenticated(authenticatedAtMs = 1),
            )
            override fun isUnlocked() = true
        }
        private val configurationSource = object : BiometricConfigurationSource {
            override val biometricConfiguration = configuration
        }
        fun model() = SecuritySettingsViewModel(
            SecurityAuthenticationSettingsInteractor(
                object : AuthenticationMethodAvailability { override val methods = this@Fixture.methods },
                session, provisioner, configurationSource,
            ),
            configurationSource,
            repository,
        ).also { it.onPageEntered() }
    }

    private class Provisioner(private val operation: suspend () -> AuthenticationResult) : AuthenticationMethodProvisioner {
        var calls = 0
        override suspend fun rotateBiometricPolicy(invalidateOnEnrollment: Boolean): AuthenticationResult {
            calls++
            return operation()
        }
        override suspend fun disableBiometric(): AuthenticationResult {
            calls++
            return operation()
        }
        override suspend fun authorizeAppPasswordManagement() = error("unused")
        override suspend fun setAppPassword(password: CharArray) = error("unused")
        override suspend fun changeAppPassword(currentPassword: CharArray, newPassword: CharArray) = error("unused")
        override suspend fun disableAppPassword() = error("unused")
        override suspend fun hasRecoveryCode() = false
        override suspend fun checkRecoveryCode(code: CharArray) = false
    }

    private class Repository(private val save: suspend () -> Unit) : SecuritySettingsRepository {
        override val security = MutableStateFlow(SecuritySettings())
        override suspend fun setSecureContentEnabled(enabled: Boolean) = Unit
        override suspend fun setFlipToLockEnabled(enabled: Boolean) = Unit
        override suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean) = Unit
        override suspend fun setLockOnBackground(enabled: Boolean) = save()
        override suspend fun setLockTimeout(timeoutMs: Long) = save()
        override suspend fun setReauthenticateSensitiveCopies(enabled: Boolean) = Unit
        override suspend fun setClipboardClearEnabled(enabled: Boolean) = Unit
        override suspend fun setClipboardClearDelaySeconds(delaySeconds: Int) = Unit
    }
}
