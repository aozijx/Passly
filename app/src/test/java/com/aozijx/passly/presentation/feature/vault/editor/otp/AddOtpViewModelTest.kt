package com.aozijx.passly.presentation.feature.vault.editor.otp

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
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.Rule

class AddOtpViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun scannedHotp_roundTripsThroughFormDraftAndMaterializer() {
        val viewModel = AddOtpViewModel(createEntryUseCase(), AuthenticatedSession)
        val config = OtpConfig(
            type = OtpType.HOTP,
            secret = "ABC 123",
            algorithm = OtpHashAlgorithm.SHA512,
            digits = 8,
            periodSeconds = null,
            counter = Long.MAX_VALUE,
            encoding = OtpSecretEncoding.BASE64,
            issuer = "Example",
            accountName = "ada@example.com",
        )

        viewModel.onAction(AddOtpAction.ScannedConfigApplied(config))
        val state: AddOtpUiState = viewModel.uiState.value
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
    }

    private fun createEntryUseCase() = CreateEntryUseCase(
        entryCommandRepository = NoOpEntryCommandRepository,
        secureSessionAccessState = AuthenticatedSession,
        materializer = EntryDraftMaterializer(),
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
