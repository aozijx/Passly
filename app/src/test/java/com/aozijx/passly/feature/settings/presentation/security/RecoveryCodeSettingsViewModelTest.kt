package com.aozijx.passly.feature.settings.presentation.security

import androidx.lifecycle.SavedStateHandle
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.BiometricConfiguration
import com.aozijx.passly.domain.access.model.CancellationReason
import com.aozijx.passly.domain.access.model.RecoveryCredentialCreation
import com.aozijx.passly.domain.access.model.RecoveryCredentialDraft
import com.aozijx.passly.domain.access.model.RecoveryCredentialFactory
import com.aozijx.passly.domain.access.model.RecoveryCredentialId
import com.aozijx.passly.domain.access.port.AuthenticationMethodAvailability
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.access.port.BiometricConfigurationSource
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.settings.security.RecoveryCodeDraftInteractor
import com.aozijx.passly.feature.settings.security.SecurityAuthenticationSettingsInteractor
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecoveryCodeSettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `verification ignores input changes until the current verification completes`() =
        runTest {
            val verificationGate = CompletableDeferred<Boolean>()
            val provisioner = TestProvisioner { verificationGate.await() }
            val model = fixture(DeferredCommitDraft(CompletableDeferred()), provisioner)
            val owner = model.onPageEntered()

            model.onAction(RecoveryCodeSettingsAction.VerificationInputChanged("recovery-code"))
            val submittedInput =
                (model.uiState.value.verification as RecoveryCodeVerificationUiState.Editing).input as OwnedChars
            model.onAction(RecoveryCodeSettingsAction.Verify)
            runCurrent()
            val verifying = model.uiState.value.verification

            model.onAction(RecoveryCodeSettingsAction.VerificationInputChanged("replacement"))

            assertSame(verifying, model.uiState.value.verification)
            verificationGate.complete(true)
            runCurrent()

            val accepted = model.uiState.value.verification as RecoveryCodeVerificationUiState.Accepted
            assertTrue(accepted.input.isEmpty)
            assertTrue(submittedInput.isWiped)
            assertEquals("recovery-code", provisioner.verifiedCode)

            model.onPageExited(owner)
        }

    @Test
    fun `page exit wipes ready disclosure and verification input and clears saved disclosure marker`() =
        runTest {
            val savedState = SavedStateHandle()
            val draft = DeferredCommitDraft(CompletableDeferred())
            val model = fixture(draft, savedState = savedState)
            val owner = model.onPageEntered()
            model.onAction(RecoveryCodeSettingsAction.VerificationInputChanged("old-input"))
            val input = model.uiState.value.verification.input as OwnedChars
            model.onAction(RecoveryCodeSettingsAction.Generate)
            runCurrent()
            val disclosure = (model.uiState.value.draft as RecoveryCodeDraftUiState.Ready).disclosure
            assertEquals(true, savedState.get<Boolean>("wasRecoveryDisclosureOpen"))

            model.onPageExited(owner)

            assertEquals(RecoveryCodeDraftUiState.Empty, model.uiState.value.draft)
            assertEquals(true, model.uiState.value.verification.input.isEmpty)
            assertEquals(true, input.isWiped)
            assertEquals(true, disclosure.toCharArray().isEmpty())
            assertEquals(null, savedState.get<Boolean>("wasRecoveryDisclosureOpen"))
            assertEquals(1, draft.closeCalls)

            model.onAction(RecoveryCodeSettingsAction.VerificationInputChanged("late-input"))
            assertEquals(true, model.uiState.value.verification.input.isEmpty)
        }

    @Test
    fun `late verification after exit cannot overwrite input on reentered page`() =
        runTest {
            val verificationGate = CompletableDeferred<Boolean>()
            val provisioner = TestProvisioner { withContext(NonCancellable) { verificationGate.await() } }
            val model = fixture(DeferredCommitDraft(CompletableDeferred()), provisioner)
            val oldOwner = model.onPageEntered()
            model.onAction(RecoveryCodeSettingsAction.VerificationInputChanged("old-code"))
            val oldInput = model.uiState.value.verification.input as OwnedChars
            model.onAction(RecoveryCodeSettingsAction.Verify)
            runCurrent()

            model.onPageExited(oldOwner)
            assertEquals(true, oldInput.isWiped)
            assertEquals(true, model.uiState.value.verification.input.isEmpty)
            model.onPageEntered()
            model.onAction(RecoveryCodeSettingsAction.VerificationInputChanged("new-code"))
            verificationGate.complete(true)
            runCurrent()

            val current = model.uiState.value.verification as RecoveryCodeVerificationUiState.Editing
            assertEquals("new-code", current.input.toCharArray().concatToString())
            assertEquals(false, current.input.isEmpty)
            assertEquals(true, provisioner.verifiedChars?.all { it == '\u0000' })
        }

    @Test
    fun `rejected verification input is wiped when page exits`() =
        runTest {
            val model = fixture(DeferredCommitDraft(CompletableDeferred()))
            val owner = model.onPageEntered()
            model.onAction(RecoveryCodeSettingsAction.VerificationInputChanged("rejected-code"))
            val input = model.uiState.value.verification.input as OwnedChars
            model.onAction(RecoveryCodeSettingsAction.Verify)
            runCurrent()
            assertEquals(true, model.uiState.value.verification is RecoveryCodeVerificationUiState.Rejected)

            model.onPageExited(owner)

            assertEquals(true, input.isWiped)
            assertEquals(true, model.uiState.value.verification.input.isEmpty)
        }

    @Test
    fun `failed verification input is wiped when page exits`() =
        runTest {
            val provisioner = TestProvisioner { throw IllegalStateException("verification failed") }
            val model = fixture(DeferredCommitDraft(CompletableDeferred()), provisioner)
            val owner = model.onPageEntered()
            model.onAction(RecoveryCodeSettingsAction.VerificationInputChanged("failed-code"))
            val input = model.uiState.value.verification.input as OwnedChars
            model.onAction(RecoveryCodeSettingsAction.Verify)
            runCurrent()
            assertEquals(true, model.uiState.value.verification is RecoveryCodeVerificationUiState.Failed)

            model.onPageExited(owner)

            assertEquals(true, input.isWiped)
            assertEquals(true, model.uiState.value.verification.input.isEmpty)
        }

    @Test
    fun `confirmed commit continues after page exit without retaining disclosure`() =
        runTest {
            val savedState = SavedStateHandle()
            val commitGate = CompletableDeferred<Unit>()
            val draft = DeferredCommitDraft(commitGate)
            val model = fixture(draft, savedState = savedState)
            val owner = model.onPageEntered()
            model.onAction(RecoveryCodeSettingsAction.Generate)
            runCurrent()
            val disclosure = (model.uiState.value.draft as RecoveryCodeDraftUiState.Ready).disclosure
            model.onAction(RecoveryCodeSettingsAction.ConfirmAndEnable)
            runCurrent()
            assertSame(disclosure, (model.uiState.value.draft as RecoveryCodeDraftUiState.Committing).disclosure)

            model.onPageExited(owner)

            assertEquals(null, (model.uiState.value.draft as RecoveryCodeDraftUiState.Committing).disclosure)
            assertEquals(true, disclosure.toCharArray().isEmpty())
            assertEquals(0, draft.closeCalls)
            model.onAction(RecoveryCodeSettingsAction.Generate)
            commitGate.complete(Unit)
            runCurrent()

            assertEquals(RecoveryCodeDraftUiState.Empty, model.uiState.value.draft)
            assertEquals(true, model.uiState.value.hasRecoveryCode)
            assertEquals(null, savedState.get<Boolean>("wasRecoveryDisclosureOpen"))
            assertEquals(0, draft.closeCalls)
        }

    @Test
    fun `cancelled confirmed commit after page exit closes draft without restoring disclosure`() =
        runTest {
            val commitGate = CompletableDeferred<Unit>()
            val draft =
                DeferredCommitDraft(
                    commitGate,
                    commitResult = AuthenticationResult.Cancelled(CancellationReason.USER),
                )
            val model = fixture(draft)
            val owner = model.onPageEntered()
            model.onAction(RecoveryCodeSettingsAction.Generate)
            runCurrent()
            model.onAction(RecoveryCodeSettingsAction.ConfirmAndEnable)
            runCurrent()
            model.onPageExited(owner)

            commitGate.complete(Unit)
            runCurrent()

            assertEquals(RecoveryCodeDraftUiState.Empty, model.uiState.value.draft)
            assertEquals(false, model.uiState.value.hasRecoveryCode)
            assertEquals(1, draft.closeCalls)
        }

    @Test
    fun `cancelled commit after durable save reconciles recovery code state before propagating`() =
        runTest {
            val draft =
                DeferredCommitDraft(
                    commitGate = CompletableDeferred(),
                    commitFailure = kotlinx.coroutines.CancellationException("cancelled after save"),
                    commitBeforeFailure = true,
                )
            val savedState = SavedStateHandle()
            val model = fixture(draft, savedState = savedState)
            model.onPageEntered()
            model.onAction(RecoveryCodeSettingsAction.Generate)
            runCurrent()
            model.onAction(RecoveryCodeSettingsAction.ConfirmAndEnable)
            runCurrent()

            draft.commitGate.complete(Unit)
            runCurrent()

            assertEquals(true, model.uiState.value.hasRecoveryCode)
            assertEquals(RecoveryCodeDraftUiState.Committed, model.uiState.value.draft)
            assertEquals(false, savedState.get<Boolean>("wasRecoveryDisclosureOpen"))
            assertEquals(true, draft.durableCommit)
        }

    @Test
    fun `fatal commit error reconciles durable state before coroutine termination`() {
        val fatal = AssertionError("fatal after durable commit")
        val actual =
            runCatching {
                runTest {
                    val draft =
                        DeferredCommitDraft(
                            commitGate = CompletableDeferred(),
                            commitFailure = fatal,
                            commitBeforeFailure = true,
                        )
                    val model = fixture(draft)
                    model.onPageEntered()
                    model.onAction(RecoveryCodeSettingsAction.Generate)
                    runCurrent()
                    model.onAction(RecoveryCodeSettingsAction.ConfirmAndEnable)
                    runCurrent()
                    draft.commitGate.complete(Unit)
                    runCurrent()

                    assertEquals(true, model.uiState.value.hasRecoveryCode)
                    assertEquals(RecoveryCodeDraftUiState.Committed, model.uiState.value.draft)
                }
            }.exceptionOrNull()

        assertSame(fatal, actual)
    }

    @Test
    fun `failed confirmed commit after page exit closes draft without restoring disclosure`() =
        runTest {
            val commitGate = CompletableDeferred<Unit>()
            val draft =
                DeferredCommitDraft(
                    commitGate,
                    commitResult =
                        AuthenticationResult.Failure(
                            AuthenticationFailure(AuthenticationFailureCode.LAST_METHOD_REQUIRED),
                        ),
                )
            val model = fixture(draft)
            val owner = model.onPageEntered()
            model.onAction(RecoveryCodeSettingsAction.Generate)
            runCurrent()
            model.onAction(RecoveryCodeSettingsAction.ConfirmAndEnable)
            runCurrent()
            model.onPageExited(owner)

            commitGate.complete(Unit)
            runCurrent()

            assertEquals(RecoveryCodeDraftUiState.Empty, model.uiState.value.draft)
            assertEquals(false, model.uiState.value.hasRecoveryCode)
            assertEquals(1, draft.closeCalls)
        }

    @Test
    fun `old page exit cannot wipe a newer page visit`() =
        runTest {
            val model = fixture(DeferredCommitDraft(CompletableDeferred()))
            val oldOwner = model.onPageEntered()
            model.onPageExited(oldOwner)
            val newOwner = model.onPageEntered()
            model.onAction(RecoveryCodeSettingsAction.VerificationInputChanged("current-input"))
            val input = model.uiState.value.verification.input

            model.onPageExited(oldOwner)

            assertSame(input, model.uiState.value.verification.input)
            assertEquals("current-input", input.toCharArray().concatToString())
            model.onPageExited(newOwner)
        }

    @Test
    fun `confirm is single flight and draft cannot be dismissed while commit is pending`() =
        runTest {
            val commitGate = CompletableDeferred<Unit>()
            val draft = DeferredCommitDraft(commitGate)
            val model =
                fixture(draft).also {
                    it.onPageEntered()
                    it.onAction(RecoveryCodeSettingsAction.Generate)
                }
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
        savedState: SavedStateHandle = SavedStateHandle(),
    ): RecoveryCodeSettingsViewModel {
        val authentication =
            SecurityAuthenticationSettingsInteractor(
                authenticationMethodAvailability =
                    object : AuthenticationMethodAvailability {
                        override val methods =
                            MutableStateFlow(
                                com.aozijx.passly.domain.access.model
                                    .AuthenticationMethods(),
                            )
                    },
                secureSessionAccessState =
                    object : SecureSessionAccessState {
                        override val authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Locked)

                        override fun isUnlocked() = false
                    },
                authenticationMethodProvisioner = provisioner,
                configurationSource =
                    object : BiometricConfigurationSource {
                        override val biometricConfiguration: Flow<BiometricConfiguration> =
                            flowOf(BiometricConfiguration.NotConfigured)
                    },
            )
        val interactor =
            RecoveryCodeDraftInteractor(
                draftFactory = RecoveryCredentialFactory { RecoveryCredentialCreation.Ready(draft) },
                clipboardWriter = SensitiveClipboardWriter {},
            )
        return RecoveryCodeSettingsViewModel(savedState, interactor, authentication)
    }

    private class DeferredCommitDraft(
        val commitGate: CompletableDeferred<Unit>,
        private val commitResult: AuthenticationResult =
            AuthenticationResult.Success(AuthenticationMethod.RECOVERY_CODE),
        private val commitFailure: Throwable? = null,
        private val commitBeforeFailure: Boolean = false,
    ) : RecoveryCredentialDraft {
        override val id = RecoveryCredentialId("test-draft")
        var commitCalls = 0
            private set
        var closeCalls = 0
            private set
        var durableCommit = false
            private set

        override fun reveal() = "recovery-code".toCharArray()

        override suspend fun commit(): AuthenticationResult {
            commitCalls += 1
            commitGate.await()
            commitFailure?.let {
                durableCommit = commitBeforeFailure
                throw it
            }
            durableCommit = commitResult is AuthenticationResult.Success
            return commitResult
        }

        override val isCommitted: Boolean get() = durableCommit

        override fun close() {
            closeCalls += 1
        }
    }

    private class TestProvisioner(
        private val verify: suspend () -> Boolean = { false },
    ) : AuthenticationMethodProvisioner {
        var verifiedCode: String? = null
            private set
        var verifiedChars: CharArray? = null
            private set

        override suspend fun authorizeAppPasswordManagement() = error("Unused")

        override suspend fun setAppPassword(password: CharArray) = error("Unused")

        override suspend fun changeAppPassword(
            currentPassword: CharArray,
            newPassword: CharArray,
        ) = error("Unused")

        override suspend fun disableAppPassword() = error("Unused")

        override suspend fun disableBiometric() = error("Unused")

        override suspend fun rotateBiometricPolicy(invalidateOnEnrollment: Boolean) = error("Unused")

        override suspend fun hasRecoveryCode() = false

        override suspend fun checkRecoveryCode(code: CharArray): Boolean {
            verifiedCode = code.concatToString()
            verifiedChars = code
            return verify()
        }
    }
}
