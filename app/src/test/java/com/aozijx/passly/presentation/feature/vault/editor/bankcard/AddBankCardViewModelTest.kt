package com.aozijx.passly.presentation.feature.vault.editor.bankcard

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.feature.vault.editor.bankcard.CardNetwork
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.feature.vault.entry.EntryDraftMaterializer
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule

class AddBankCardViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun validCardNumberEnablesSaveAndKeepsValidationOutsideMapper() {
        val viewModel = AddBankCardViewModel(createEntryUseCase(), AuthenticatedSession)

        viewModel.onAction(AddBankCardAction.TitleChanged("Visa"))
        viewModel.onAction(AddBankCardAction.CardNumberChanged("4111 1111 1111 1111"))

        val state: AddBankCardUiState = viewModel.uiState.value
        assertTrue(state.canSave)
        assertNull(state.form.cardNumberError)
        assertEquals(CardNetwork.VISA, state.form.inferredNetwork)
        assertEquals("4111 1111 1111 1111", state.form.cardNumber)
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
