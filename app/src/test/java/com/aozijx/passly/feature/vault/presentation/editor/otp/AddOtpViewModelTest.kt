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
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.feature.vault.entry.EntryDraftMaterializer
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveEffect
import com.aozijx.passly.testing.testLogger
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule

class AddOtpViewModelTest {
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

        viewModel.onAction(AddOtpAction.ScannedConfigApplied(config))
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
    fun saveCapabilityIsDerivedFromFormAndSavePhase() {
        val form = OtpFormState(title = "Mail", secret = "JBSWY3DPEHPK3PXP")

        assertTrue(AddOtpCodeState(form = form).canSave)
        assertTrue(
            !AddOtpCodeState(form = form, savePhase = AddOtpSavePhase.Saving).canSave,
        )
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

    private fun createEntryUseCase(repository: EntryCommandRepository = NoOpEntryCommandRepository) = CreateEntryUseCase(
        entryCommandRepository = repository,
        secureSessionAccessState = AuthenticatedSession,
        materializer = EntryDraftMaterializer(),
        logger = testLogger(),
    )

    private object AuthenticatedSession : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.Authenticated(1L))
        override fun isUnlocked() = true
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
