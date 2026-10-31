package com.aozijx.passly.feature.settings.presentation.security

import androidx.lifecycle.ViewModelStore
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.CancellationReason
import com.aozijx.passly.domain.access.port.AuthenticationMethodAvailability
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.feature.settings.security.AppPasswordChangeRequest
import com.aozijx.passly.feature.settings.security.AppPasswordSettingsInteractor
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppPasswordSettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `submit received after page exit wipes request without starting provisioner`() = runTest {
        val provisioner = Provisioner()
        val viewModel = viewModel(provisioner)
        val owner = viewModel.onPageEntered()
        viewModel.onPageExited(owner)
        val password = "long-enough-password".toCharArray()
        val confirmation = "long-enough-password".toCharArray()

        viewModel.onAction(
            AppPasswordSettingsAction.SubmitChange(
                AppPasswordChangeRequest.Set(password, confirmation),
            ),
        )
        runCurrent()

        assertTrue(password.all { it == '\u0000' })
        assertTrue(confirmation.all { it == '\u0000' })
        assertEquals(0, provisioner.setCalls)
    }

    @Test
    fun `view model cleared before operation starts wipes request buffers`() = runTest {
        val provisioner = Provisioner()
        val viewModel = viewModel(provisioner)
        val store = ViewModelStore().apply { put("app-password", viewModel) }
        viewModel.onPageEntered()
        val password = "long-enough-password".toCharArray()
        val confirmation = "long-enough-password".toCharArray()

        viewModel.onAction(
            AppPasswordSettingsAction.SubmitChange(
                AppPasswordChangeRequest.Set(password, confirmation),
            ),
        )
        store.clear()
        runCurrent()

        assertTrue(password.all { it == '\u0000' })
        assertTrue(confirmation.all { it == '\u0000' })
        assertEquals(0, provisioner.setCalls)
    }

    @Test
    fun `only one password operation runs and duplicate request buffers are wiped`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val provisioner = Provisioner {
            gate.await()
            AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)
        }
        val viewModel = viewModel(provisioner)
        viewModel.onPageEntered()
        val effects = mutableListOf<AppPasswordSettingsEffect>()
        backgroundScope.launch { viewModel.effects.toList(effects) }
        runCurrent()
        val firstPassword = "long-enough-password".toCharArray()
        val firstConfirmation = "long-enough-password".toCharArray()

        viewModel.onAction(
            AppPasswordSettingsAction.SubmitChange(
                AppPasswordChangeRequest.Set(firstPassword, firstConfirmation),
            ),
        )
        runCurrent()

        assertEquals(1, provisioner.setCalls)
        assertTrue(viewModel.uiState.value.isOperationInProgress)
        val duplicatePassword = "another-long-password".toCharArray()
        val duplicateConfirmation = "another-long-password".toCharArray()
        viewModel.onAction(
            AppPasswordSettingsAction.SubmitChange(
                AppPasswordChangeRequest.Set(duplicatePassword, duplicateConfirmation),
            ),
        )
        runCurrent()

        assertEquals(1, provisioner.setCalls)
        assertTrue(duplicatePassword.all { it == '\u0000' })
        assertTrue(duplicateConfirmation.all { it == '\u0000' })
        gate.complete(Unit)
        runCurrent()

        assertEquals(listOf(AppPasswordSettingsEffect.AppPasswordSet), effects)
        assertEquals(false, viewModel.uiState.value.isOperationInProgress)
    }

    @Test
    fun `result from exited page is not delivered to the reentered page`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val provisioner = Provisioner {
            gate.await()
            AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)
        }
        val viewModel = viewModel(provisioner)
        val oldOwner = viewModel.onPageEntered()
        val effects = mutableListOf<AppPasswordSettingsEffect>()
        backgroundScope.launch { viewModel.effects.toList(effects) }
        runCurrent()
        val password = "long-enough-password".toCharArray()
        val confirmation = "long-enough-password".toCharArray()
        viewModel.onAction(
            AppPasswordSettingsAction.SubmitChange(
                AppPasswordChangeRequest.Set(password, confirmation),
            ),
        )
        runCurrent()

        viewModel.onPageExited(oldOwner)
        viewModel.onPageEntered()
        gate.complete(Unit)
        runCurrent()

        assertTrue(password.all { it == '\u0000' })
        assertTrue(confirmation.all { it == '\u0000' })
        assertTrue(effects.isEmpty())
        assertEquals(false, viewModel.uiState.value.isOperationInProgress)
    }

    private fun viewModel(provisioner: Provisioner) = AppPasswordSettingsViewModel(
        AppPasswordSettingsInteractor(
            authenticationMethodAvailability = object : AuthenticationMethodAvailability {
                override val methods = MutableStateFlow(AuthenticationMethods())
            },
            authenticationMethodProvisioner = provisioner,
        ),
    )

    private class Provisioner(
        private val setPassword: suspend () -> AuthenticationResult = {
            AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)
        },
    ) : AuthenticationMethodProvisioner {
        var setCalls = 0

        override suspend fun authorizeAppPasswordManagement() =
            AuthenticationResult.Cancelled(CancellationReason.USER)

        override suspend fun setAppPassword(password: CharArray): AuthenticationResult {
            setCalls++
            return setPassword()
        }

        override suspend fun changeAppPassword(
            currentPassword: CharArray,
            newPassword: CharArray,
        ) = AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)

        override suspend fun disableAppPassword() =
            AuthenticationResult.Cancelled(CancellationReason.USER)

        override suspend fun disableBiometric() =
            AuthenticationResult.Cancelled(CancellationReason.USER)

        override suspend fun rotateBiometricPolicy(invalidateOnEnrollment: Boolean) =
            AuthenticationResult.Cancelled(CancellationReason.USER)

        override suspend fun hasRecoveryCode() = false

        override suspend fun checkRecoveryCode(code: CharArray) = false
    }
}
