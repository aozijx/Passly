package com.aozijx.passly.presentation.feature.settings.autofill

import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.port.InteractionSettingsRepository
import com.aozijx.passly.feature.autofill.platform.AutofillServiceStatusSource
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutofillSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `remains loading until persisted preferences arrive`() = runTest {
        val repository = RecordingInteractionSettingsRepository(emptyFlow())
        val viewModel = AutofillSettingsViewModel(
            settingsRepository = repository,
            autofillServiceStatusSource = AutofillServiceStatusSource { MutableStateFlow(false) },
        )

        assertEquals(AutofillSettingsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `save prompt action updates only the legacy save preference`() = runTest {
        val repository = RecordingInteractionSettingsRepository()
        val viewModel = AutofillSettingsViewModel(
            settingsRepository = repository,
            autofillServiceStatusSource = AutofillServiceStatusSource { MutableStateFlow(false) },
        )

        viewModel.onAction(AutofillSettingsAction.SetSavePromptsEnabled(false))
        advanceUntilIdle()

        assertEquals(listOf(false), repository.savePromptWrites)
        assertEquals(0, repository.credentialManagerWrites)
    }

    private class RecordingInteractionSettingsRepository(
        override val interaction: kotlinx.coroutines.flow.Flow<InteractionSettings> =
            MutableStateFlow(InteractionSettings()),
    ) : InteractionSettingsRepository {
        val savePromptWrites = mutableListOf<Boolean>()
        var credentialManagerWrites = 0

        override suspend fun setAutofillSavePromptsEnabled(enabled: Boolean) {
            savePromptWrites += enabled
        }

        override suspend fun setCredentialManagerEnabled(enabled: Boolean) {
            credentialManagerWrites++
        }

        override suspend fun setSwipeEnabled(enabled: Boolean) = Unit
        override suspend fun setPullToSearchEnabled(enabled: Boolean) = Unit
        override suspend fun setSwipeLeftAction(action: SwipeActionType) = Unit
        override suspend fun setSwipeRightAction(action: SwipeActionType) = Unit
        override suspend fun setAutofillEnabled(enabled: Boolean) = Unit
        override suspend fun setAutofillPresentation(presentation: AutofillPresentation) = Unit
        override suspend fun setAutofillAuthenticationRequired(required: Boolean) = Unit
        override suspend fun setAutofillOtpEnabled(enabled: Boolean) = Unit
        override suspend fun setUnmatchedAutofillSuggestionsEnabled(enabled: Boolean) = Unit
        override suspend fun setAutofillMaxSuggestions(count: Int) = Unit
    }
}
