package com.aozijx.passly.feature.vault.presentation.editor.bankcard

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.credential.CardExpiry
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.policy.CardNetwork
import com.aozijx.passly.domain.entry.policy.CardNetworkMatch
import com.aozijx.passly.domain.entry.policy.CardNumberValidationResult
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.feature.vault.detail.EditableEntryUseCaseFixture
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase
import com.aozijx.passly.domain.entry.policy.EntryDraftMaterializer
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveEffect
import com.aozijx.passly.testing.testLogger
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class AddBankCardViewModelTest {
    @Test
    fun fullEditRevealsCardSecretsAndSavesTheWholeCardForm() = runTest {
        val entry = Entry(
            identity = EntryIdentity(EntryId("card-edit"), EntryType.BANK_CARD, timestamps = EntryTimestamps(1)),
            profile = EntryProfile("Card"),
            secret = EntrySecret(credential = CardCredential(cardNumber = null, cardCvv = null)),
        )
        val fixture = EditableEntryUseCaseFixture(
            entry,
            setOf(SensitiveFieldKey.CARD_NUMBER, SensitiveFieldKey.CARD_CVV),
            mapOf(
                SensitiveFieldKey.CARD_NUMBER to "4111111111111111",
                SensitiveFieldKey.CARD_CVV to "123",
            ),
        )
        val model = AddBankCardViewModel(
            createEntryUseCase(), fixture.access,
            loadEditableEntry = fixture.loader,
            editDetailEntry = fixture.editor,
        )

        model.loadExistingEntry("card-edit")
        advanceUntilIdle()
        assertEquals(EditorLoadPhase.Ready, model.uiState.value.loadPhase)
        assertEquals("4111111111111111", model.uiState.value.form.cardNumber)
        assertEquals("123", model.uiState.value.form.cardCvv)

        model.onAction(AddBankCardAction.CardholderChanged("Ada"))
        model.onAction(AddBankCardAction.Save)
        advanceUntilIdle()
        assertEquals("Ada", fixture.query.current?.secret?.card?.cardHolder)
        assertEquals("4111111111111111", fixture.query.current?.secret?.card?.cardNumber)
        assertEquals("123", fixture.query.current?.secret?.card?.cardCvv)
    }

    @Test
    fun `invalid cvv does not attempt save or emit failure and correction saves normally`() = runTest {
        val created = mutableListOf<Entry>()
        val repository = object : EntryCommandRepository by NoOpEntryCommandRepository {
            override suspend fun createEntry(entry: Entry): AppResult<EntryId> {
                created += entry
                return AppResult.Success(entry.id)
            }
        }
        val model = AddBankCardViewModel(createEntryUseCase(repository), AuthenticatedSession)
        val effects = mutableListOf<EditorSaveEffect>()
        backgroundScope.launch { model.effects.toList(effects) }
        model.onAction(AddBankCardAction.TitleChanged("Card"))
        model.onAction(AddBankCardAction.CardNumberChanged("4111111111111111"))
        model.onAction(AddBankCardAction.CvvChanged("12"))
        model.onAction(AddBankCardAction.Save)
        runCurrent()
        assertEquals(0, created.size)
        assertEquals(emptyList<EditorSaveEffect>(), effects)
        assertEquals(AddBankCardSavePhase.Idle, model.uiState.value.savePhase)
        model.onAction(AddBankCardAction.CvvChanged(" 123 "))
        model.onAction(AddBankCardAction.Save)
        runCurrent()
        assertEquals("123", created.single().secret.card?.cardCvv)
        assertEquals(listOf(EditorSaveEffect.Saved), effects)
    }

    @Test
    fun `invalid optional cvv disables save until corrected or cleared`() {
        val model = AddBankCardViewModel(createEntryUseCase(), AuthenticatedSession)
        model.onAction(AddBankCardAction.TitleChanged("Card"))
        model.onAction(AddBankCardAction.CardNumberChanged("4111111111111111"))
        for (value in listOf("12", "12345", "1 23", "１２３", "abc")) {
            model.onAction(AddBankCardAction.CvvChanged(value))
            assertTrue(!model.uiState.value.canSave)
        }
        for (value in listOf("123", "1234", " 123 ", "", "   ")) {
            model.onAction(AddBankCardAction.CvvChanged(value))
            assertTrue(model.uiState.value.canSave)
        }
    }

    @Test
    fun `copied card form derives number validation from the replacement input`() {
        val form = AddBankCardFormState(
            title = "Card", cardNumber = "4111111111111111",
        )
        assertTrue(!form.copy(cardNumber = "invalid").isValid)
    }

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
