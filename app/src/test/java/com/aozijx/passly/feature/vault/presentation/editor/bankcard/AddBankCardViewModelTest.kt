package com.aozijx.passly.feature.vault.presentation.editor.bankcard

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.credential.CardExpiry
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.policy.CardNetwork
import com.aozijx.passly.domain.entry.policy.CardNetworkMatch
import com.aozijx.passly.domain.entry.policy.CardNumberValidationResult
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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule

class AddBankCardViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun savingCardWithSelectedExpiryPassesDomainValidationAndPersistsOnce() = runTest {
        val created = mutableListOf<Entry>()
        val repository = object : EntryCommandRepository by NoOpEntryCommandRepository {
            override suspend fun createEntry(entry: Entry): AppResult<EntryId> {
                created += entry
                return AppResult.Success(entry.id)
            }
        }
        val viewModel = AddBankCardViewModel(createEntryUseCase(repository), AuthenticatedSession)
        viewModel.onAction(AddBankCardAction.TitleChanged("Visa"))
        viewModel.onAction(AddBankCardAction.CardNumberChanged("4111 1111 1111 1111"))
        viewModel.onAction(AddBankCardAction.IssuingBankChanged("Example Bank"))
        viewModel.onAction(AddBankCardAction.BankBranchChanged("Central Branch"))
        viewModel.onAction(AddBankCardAction.CardProductChanged("Travel Platinum"))
        viewModel.onAction(AddBankCardAction.CurrencyChanged("CNY / USD"))
        viewModel.onAction(AddBankCardAction.BankPhoneChanged("+86 123456"))
        viewModel.onAction(AddBankCardAction.ExpiryChanged(CardExpirySelection.Selected(CardExpiry(month = 3, year = 2030))))

        viewModel.onAction(AddBankCardAction.Save)

        assertEquals(EditorSaveEffect.Saved, viewModel.effects.first())
        assertEquals("03/2030", created.single().secret.card?.cardExpiry)
        assertEquals("Example Bank", created.single().secret.card?.issuingBank)
        assertEquals("Central Branch", created.single().secret.card?.bankBranch)
        assertEquals("Travel Platinum", created.single().secret.card?.cardProduct)
        assertEquals("CNY / USD", created.single().secret.card?.currency)
        assertEquals("+86 123456", created.single().secret.card?.bankPhone)
        assertEquals("", viewModel.uiState.value.form.issuingBank)
        assertEquals("", viewModel.uiState.value.form.cardNumber)
    }

    @Test
    fun validCardNumberEnablesSaveAndKeepsValidationOutsideMapper() {
        val viewModel = AddBankCardViewModel(createEntryUseCase(), AuthenticatedSession)

        viewModel.onAction(AddBankCardAction.TitleChanged("Visa"))
        viewModel.onAction(AddBankCardAction.CardNumberChanged("4111 1111 1111 1111"))

        val state: AddBankCardUiState = viewModel.uiState.value
        assertTrue(state.canSave)
        assertEquals(
            CardNumberValidationResult.Valid(CardNetworkMatch.Matched(CardNetwork.VISA)),
            state.form.cardNumberValidation,
        )
        assertEquals("4111 1111 1111 1111", state.form.cardNumber)
        assertEquals(AddBankCardSavePhase.Idle, state.savePhase)
    }

    @Test
    fun saveCapabilityIsDerivedFromValidationAndSavePhase() {
        val form = AddBankCardFormState(
            title = "Visa",
            cardNumber = "4111111111111111",
            cardNumberValidation = CardNumberValidationResult.Valid(
                CardNetworkMatch.Matched(CardNetwork.VISA),
            ),
        )

        assertTrue(AddBankCardUiState(form = form).canSave)
        assertTrue(
            !AddBankCardUiState(
                form = form,
                savePhase = AddBankCardSavePhase.Saving,
            ).canSave,
        )
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
