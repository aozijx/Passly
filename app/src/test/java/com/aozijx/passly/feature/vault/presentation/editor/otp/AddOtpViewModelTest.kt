package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.otp.toNewEntryDraft
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.domain.entry.policy.EntryDraftMaterializer
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveEffect
import com.aozijx.passly.testing.testLogger
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class AddOtpViewModelTest {
    @Test
    fun `scanner editor and direct creation share complete identity title`() {
        val model = AddOtpViewModel(createEntryUseCase(), AuthenticatedSession)
        val config = OtpConfig(secret = "MY", issuer = " Example: West ", accountName = " alice:admin ")
        assertTrue(model.tryApplyScannedConfig(config))
        val directTitle = (config.toNewEntryDraft()[FieldKey.TITLE] as EntryDraftValue.Text).value
        assertEquals(directTitle, model.uiState.value.form.title)
        assertEquals("Example: West: alice:admin", directTitle)
    }

    @Test
    fun `scan without identity keeps the existing editor title`() {
        val model = AddOtpViewModel(createEntryUseCase(), AuthenticatedSession)
        model.onAction(AddOtpAction.FormChanged(OtpFormState(title = "My title")))
        assertTrue(model.tryApplyScannedConfig(OtpConfig(secret = "MY")))
        assertEquals("My title", model.uiState.value.form.title)
    }

    @Test
    fun scannedConfigAcknowledgesOnlyAcceptedApplications() = runTest {
        val session = MutableSession()
        val model = AddOtpViewModel(createEntryUseCase(session = session), session)
        val config = OtpConfig(secret = "MY")
        session.state.value = AuthenticationState.Locked
        runCurrent()
        assertFalse(model.tryApplyScannedConfig(config))
        assertEquals("", model.uiState.value.form.secret)
        session.state.value = AuthenticationState.Authenticated(2L)
        runCurrent()
        assertTrue(model.tryApplyScannedConfig(config))
        assertEquals("MY", model.uiState.value.form.secret)
    }

    @Test
    fun savingEditorRejectsScannedConfigWithoutOverwritingItsDraftOrEmittingParsed() = runTest {
        val release = CompletableDeferred<Unit>()
        val repository = object : EntryCommandRepository by NoOpEntryCommandRepository {
            override suspend fun createEntry(entry: Entry): AppResult<EntryId> {
                release.await()
                return AppResult.Success(entry.id)
            }
        }
        val model = AddOtpViewModel(createEntryUseCase(repository), AuthenticatedSession)
        model.onAction(AddOtpAction.FormChanged(OtpFormState(title = "Submitted", secret = "JBSWY3DPEHPK3PXP")))
        model.onAction(AddOtpAction.Save)
        runCurrent()
        assertFalse(model.tryApplyScannedConfig(OtpConfig(secret = "MY")))
        assertEquals("JBSWY3DPEHPK3PXP", model.uiState.value.form.secret)
        val events = mutableListOf<AddOtpEvent>()
        backgroundScope.launch { model.events.toList(events) }
        runCurrent()
        assertTrue(events.isEmpty())
        release.complete(Unit)
        runCurrent()
    }

    @Test
    fun oldParseFeedbackDoesNotSurviveLockAndReauthentication() = runTest {
        val session = MutableSession()
        val model = AddOtpViewModel(createEntryUseCase(session = session), session)
        model.onAction(AddOtpAction.UriChanged(VALID_URI, true))
        session.state.value = AuthenticationState.Locked
        runCurrent()
        session.state.value = AuthenticationState.Authenticated(2L)
        runCurrent()
        val events = mutableListOf<AddOtpEvent>()
        backgroundScope.launch { model.events.toList(events) }
        runCurrent()
        assertTrue(events.isEmpty())
    }

    @Test
    fun manualEditingInvalidatesBufferedParseFeedback() = runTest {
        val model = AddOtpViewModel(createEntryUseCase(), AuthenticatedSession)
        model.onAction(AddOtpAction.UriChanged(VALID_URI, true))
        model.onAction(AddOtpAction.FormChanged(model.uiState.value.form.copy(title = "New draft")))
        val events = mutableListOf<AddOtpEvent>()
        backgroundScope.launch { model.events.toList(events) }
        runCurrent()
        assertTrue(events.isEmpty())
    }

    @Test
    fun onlyTheLatestUriResultIsDeliveredToALateCollector() = runTest {
        val model = AddOtpViewModel(createEntryUseCase(), AuthenticatedSession)
        model.onAction(AddOtpAction.UriChanged("invalid", true))
        model.onAction(AddOtpAction.UriChanged(VALID_URI, true))
        val events = mutableListOf<AddOtpEvent>()
        backgroundScope.launch { model.events.toList(events) }
        runCurrent()
        assertEquals(listOf(AddOtpEvent.UriParsed), events)
    }

    @Test
    fun destroyedEditorDoesNotDeliverBufferedFailures() = runTest {
        val model = AddOtpViewModel(createEntryUseCase(), AuthenticatedSession)
        model.onAction(AddOtpAction.UriChanged("invalid", true))
        model.javaClass.getDeclaredMethod("onCleared").apply { isAccessible = true }.invoke(model)
        val events = mutableListOf<AddOtpEvent>()
        backgroundScope.launch { model.events.toList(events) }
        runCurrent()
        assertTrue(events.isEmpty())
    }
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun invalidInputCannotPersistGuessedOtpParameters() = runTest {
        val valid = OtpFormState(title = "Mail", secret = "JBSWY3DPEHPK3PXP")
        val invalidForms = listOf(
            valid.copy(period = "broken"), valid.copy(period = ""),
            valid.copy(digits = "broken"), valid.copy(digits = "9"),
            valid.copy(type = OtpType.HOTP, counter = "broken"),
            valid.copy(type = OtpType.HOTP, counter = ""),
            valid.copy(secret = "INVALID!"),
        )
        for (form in invalidForms) {
            val created = mutableListOf<Entry>()
            val repository = object : EntryCommandRepository by NoOpEntryCommandRepository {
                override suspend fun createEntry(entry: Entry): AppResult<EntryId> {
                    created += entry
                    return AppResult.Success(entry.id)
                }
            }
            val viewModel = AddOtpViewModel(createEntryUseCase(repository), AuthenticatedSession)
            viewModel.onAction(AddOtpAction.FormChanged(form))
            viewModel.onAction(AddOtpAction.Save)

            assertEquals(0, created.size)
            assertEquals(form, viewModel.uiState.value.form)
            assertTrue(!viewModel.uiState.value.canSave)
        }
    }

    @Test
    fun validManualInputPersistsChosenParametersOnceAndClearsSecret() = runTest {
        val created = mutableListOf<Entry>()
        val repository = object : EntryCommandRepository by NoOpEntryCommandRepository {
            override suspend fun createEntry(entry: Entry): AppResult<EntryId> {
                created += entry
                return AppResult.Success(entry.id)
            }
        }
        val viewModel = AddOtpViewModel(createEntryUseCase(repository), AuthenticatedSession)
        viewModel.onAction(AddOtpAction.FormChanged(OtpFormState(
            title = "Mail", secret = " my====== ", digits = "7", period = "60",
        )))
        viewModel.onAction(AddOtpAction.Save)

        assertEquals(EditorSaveEffect.Saved, viewModel.effects.first())
        val config = requireNotNull(created.single().secret.otp?.config)
        assertEquals("MY", config.secret)
        assertEquals(7, config.digits)
        assertEquals(60, config.periodSeconds)
        assertEquals("", viewModel.uiState.value.form.secret)
    }

    @Test
    fun encodedUriPopulatesTheEditorWithoutChangingPlusOrPercentCharacters() = runTest {
        val viewModel = AddOtpViewModel(createEntryUseCase(), AuthenticatedSession)

        viewModel.onAction(AddOtpAction.UriChanged(
            "otpauth://totp/Example%3Aalice+tag%2540mail?secret=JBSWY3DPEHPK3PXP&issuer=Example",
            reportFailure = true,
        ))

        assertEquals("alice+tag%40mail", viewModel.uiState.value.form.accountName)
        assertEquals("Example", viewModel.uiState.value.form.issuer)
        assertEquals(AddOtpEvent.UriParsed, viewModel.events.first())
    }

    @Test
    fun unsupportedUriReportsFailureWithoutReplacingExistingCredentials() = runTest {
        val viewModel = AddOtpViewModel(createEntryUseCase(), AuthenticatedSession)
        viewModel.onAction(AddOtpAction.FormChanged(OtpFormState(title = "Keep", secret = "Existing")))

        viewModel.onAction(AddOtpAction.UriChanged(
            "otpauth://totp/alice?secret=JBSWY3DPEHPK3PXP&algorithm=MD5",
            reportFailure = true,
        ))

        assertEquals("Existing", viewModel.uiState.value.form.secret)
        assertEquals("Keep", viewModel.uiState.value.form.title)
        assertEquals(AddOtpEvent.UriParseFailed, viewModel.events.first())
    }

    @Test
    fun scannedHotp_roundTripsThroughFormDraftAndMaterializer() {
        val viewModel = AddOtpViewModel(createEntryUseCase(), AuthenticatedSession)
        val config = OtpConfig(
            type = OtpType.HOTP,
            secret = "Zm9v",
            algorithm = OtpHashAlgorithm.SHA512,
            digits = 8,
            periodSeconds = null,
            counter = Long.MAX_VALUE,
            encoding = OtpSecretEncoding.BASE64,
            issuer = "Example",
            accountName = "ada@example.com",
        )

        assertTrue(viewModel.tryApplyScannedConfig(config))
        val state: AddOtpCodeState = viewModel.uiState.value
        val entry = EntryDraftMaterializer().materialize(
            state.form.toEntryDraft(),
            EntryIdentity(
                id = EntryId("otp-1"),
                type = EntryType.OTP,
                timestamps = EntryTimestamps(123L),
            ),
        )

        val roundTripped = requireNotNull(entry.secret.otp?.config)
        assertEquals(config.type, roundTripped.type)
        assertEquals(config.secret, roundTripped.secret)
        assertEquals(config.algorithm, roundTripped.algorithm)
        assertEquals(config.digits, roundTripped.digits)
        assertNull(roundTripped.periodSeconds)
        assertEquals(config.counter, roundTripped.counter)
        assertEquals(config.encoding, roundTripped.encoding)
        assertEquals(config.issuer, roundTripped.issuer)
        assertEquals(config.accountName, roundTripped.accountName)
        assertEquals(AddOtpSavePhase.Idle, state.savePhase)
    }

    @Test
    fun switchingFromSteamToHotpRestoresNumericDigits() {
        val viewModel = AddOtpViewModel(createEntryUseCase(), AuthenticatedSession)
        viewModel.onAction(AddOtpAction.FormChanged(OtpFormState(title = "OTP", secret = "MY")))
        viewModel.onAction(AddOtpAction.TypeChanged(OtpType.STEAM))
        assertEquals("5", viewModel.uiState.value.form.digits)
        viewModel.onAction(AddOtpAction.TypeChanged(OtpType.HOTP))
        assertEquals("6", viewModel.uiState.value.form.digits)
        assertTrue(viewModel.uiState.value.canSave)
    }

    private fun createEntryUseCase(
        repository: EntryCommandRepository = NoOpEntryCommandRepository,
        session: SecureSessionAccessState = AuthenticatedSession,
    ) = CreateEntryUseCase(
        entryCommandRepository = repository,
        secureSessionAccessState = session,
        materializer = EntryDraftMaterializer(),
        logger = testLogger(),
    )

    private object AuthenticatedSession : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.Authenticated(1L))
        override fun isUnlocked() = true
    }

    private class MutableSession : SecureSessionAccessState {
        val state = MutableStateFlow<AuthenticationState>(AuthenticationState.Authenticated(1L))
        override val authenticationState: StateFlow<AuthenticationState> = state
        override fun isUnlocked() = state.value is AuthenticationState.Authenticated
    }

    private companion object {
        const val VALID_URI = "otpauth://totp/Example?secret=JBSWY3DPEHPK3PXP"
    }

    private object NoOpEntryCommandRepository : EntryCommandRepository {
        override suspend fun createEntry(entry: Entry) = AppResult.Success(entry.id)
        override suspend fun updateEntry(id: EntryId, expectedVersion: EntryVersion, changes: EntryUpdate) =
            AppResult.Success(Unit)
        override suspend fun moveToTrash(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun restoreEntry(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun deletePermanently(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun emptyTrash() = AppResult.Success(0)
    }
}
