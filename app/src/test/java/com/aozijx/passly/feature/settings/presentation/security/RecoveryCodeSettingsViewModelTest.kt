package com.aozijx.passly.feature.settings.presentation.security

import androidx.lifecycle.SavedStateHandle
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.BiometricConfiguration
import com.aozijx.passly.domain.access.model.RecoveryCredentialCreation
import com.aozijx.passly.domain.access.model.RecoveryCredentialDraft
import com.aozijx.passly.domain.access.model.RecoveryCredentialFactory
import com.aozijx.passly.domain.access.model.RecoveryCredentialId
import com.aozijx.passly.domain.access.port.AuthenticationMethodAvailability
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.access.port.BiometricConfigurationSource
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.feature.settings.security.RecoveryCodeDraftInteractor
import com.aozijx.passly.feature.settings.security.SecurityAuthenticationSettingsInteractor
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecoveryCodeSettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `verification ignores input changes until the current verification completes`() = runTest {
        val verificationGate = CompletableDeferred<Boolean>()
        val provisioner = TestProvisioner { verificationGate.await() }
        val model = fixture(DeferredCommitDraft(CompletableDeferred()), provisioner)

        model.onAction(RecoveryCodeSettingsAction.VerificationInputChanged("recovery-code"))
        model.onAction(RecoveryCodeSettingsAction.Verify)
        runCurrent()
        val verifying = model.uiState.value.verification

        model.onAction(RecoveryCodeSettingsAction.VerificationInputChanged("replacement"))

        assertSame(verifying, model.uiState.value.verification)
        verificationGate.complete(true)
        runCurrent()

        val accepted = model.uiState.value.verification as RecoveryCodeVerificationUiState.Accepted
        assertEquals("recovery-code", accepted.input.toCharArray().concatToString())
        assertEquals("recovery-code", provisioner.verifiedCode)
    }

    @Test
    fun `confirm is single flight and draft cannot be dismissed while commit is pending`() = runTest {
        val commitGate = CompletableDeferred<Unit>()
        val draft = DeferredCommitDraft(commitGate)
        val model = fixture(draft).also { it.onAction(RecoveryCodeSettingsAction.Generate) }
        runCurrent()
        val disclosure = (model.uiState.value.draft as RecoveryCodeDraftUiState.Ready).disclosure

        model.onAction(RecoveryCodeSettingsAction.ConfirmAndEnable)
        model.onAction(RecoveryCodeSettingsAction.ConfirmAndEnable)
        runCurrent()

        assertEquals(1, draft.commitCalls)
        assertSame(disclosure, (model.uiState.value.draft as RecoveryCodeDraftUiState.Committing).disclosure)

        model.onAction(RecoveryCodeSettingsAction.Generate)
        model.onAction(RecoveryCodeSettingsAction.DismissDisclosure)
        runCurrent()
        assertSame(disclosure, (model.uiState.value.draft as RecoveryCodeDraftUiState.Committing).disclosure)
        assertEquals(0, draft.closeCalls)

        commitGate.complete(Unit)
        runCurrent()
        assertEquals(RecoveryCodeDraftUiState.Committed, model.uiState.value.draft)
        assertEquals(true, model.uiState.value.hasRecoveryCode)
        assertEquals(0, draft.closeCalls)
    }

    private fun fixture(
        draft: DeferredCommitDraft,
        provisioner: TestProvisioner = TestProvisioner(),
    ): RecoveryCodeSettingsViewModel {
        val authentication = SecurityAuthenticationSettingsInteractor(
            authenticationMethodAvailability = object : AuthenticationMethodAvailability {
                override val methods = MutableStateFlow(com.aozijx.passly.domain.access.model.AuthenticationMethods())
            },
            secureSessionAccessState = object : SecureSessionAccessState {
                override val authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Locked)
                override fun isUnlocked() = false
            },
            authenticationMethodProvisioner = provisioner,
            configurationSource = object : BiometricConfigurationSource {
                override val biometricConfiguration: Flow<BiometricConfiguration> =
                    flowOf(BiometricConfiguration.NotConfigured)
            },
        )
        val interactor = RecoveryCodeDraftInteractor(
            draftFactory = RecoveryCredentialFactory { RecoveryCredentialCreation.Ready(draft) },
            clipboardWriter = SensitiveClipboardWriter {},
        )
        return RecoveryCodeSettingsViewModel(SavedStateHandle(), interactor, authentication)
    }

    private class DeferredCommitDraft(
        private val commitGate: CompletableDeferred<Unit>,
    ) : RecoveryCredentialDraft {
        override val id = RecoveryCredentialId("test-draft")
        var commitCalls = 0
            private set
        var closeCalls = 0
            private set

        override fun reveal() = "recovery-code".toCharArray()

        override suspend fun commit(): AuthenticationResult {
            commitCalls += 1
            commitGate.await()
            return AuthenticationResult.Success(AuthenticationMethod.RECOVERY_CODE)
        }

        override fun close() {
            closeCalls += 1
        }
    }

    private class TestProvisioner(
        private val verify: suspend () -> Boolean = { false },
    ) : AuthenticationMethodProvisioner {
        var verifiedCode: String? = null
            private set

        override suspend fun authorizeAppPasswordManagement() = error("Unused")
        override suspend fun setAppPassword(password: CharArray) = error("Unused")
        override suspend fun changeAppPassword(currentPassword: CharArray, newPassword: CharArray) = error("Unused")
        override suspend fun disableAppPassword() = error("Unused")
        override suspend fun disableBiometric() = error("Unused")
        override suspend fun rotateBiometricPolicy(invalidateOnEnrollment: Boolean) = error("Unused")
        override suspend fun hasRecoveryCode() = false
        override suspend fun checkRecoveryCode(code: CharArray): Boolean {
            verifiedCode = code.concatToString()
            return verify()
        }
    }
}
