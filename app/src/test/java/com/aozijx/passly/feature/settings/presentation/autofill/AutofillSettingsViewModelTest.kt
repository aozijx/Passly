package com.aozijx.passly.feature.settings.presentation.autofill

import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import java.io.IOException
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.port.InteractionSettingsRepository
import com.aozijx.passly.feature.autofill.platform.AutofillServiceStatusSource
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
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

    @Test
    fun `all autofill preference failures are reported without changing source state or opening settings`() = runTest {
        val repository = RecordingInteractionSettingsRepository(save = { throw IOException("synthetic failure") })
        val model = AutofillSettingsViewModel(repository, AutofillServiceStatusSource { MutableStateFlow(false) })
        val effects = mutableListOf<AutofillSettingsEffect>()
        backgroundScope.launch { model.effects.toList(effects) }
        backgroundScope.launch { model.uiState.collect {} }
        runCurrent()
        val before = model.uiState.value
        val actions = listOf(
            AutofillSettingsAction.SetEnabled(false),
            AutofillSettingsAction.SetPresentation(AutofillPresentation.entries.first()),
            AutofillSettingsAction.SetCredentialManagerEnabled(false),
            AutofillSettingsAction.SetAuthenticationRequired(false),
            AutofillSettingsAction.SetOtpEnabled(false),
            AutofillSettingsAction.SetSavePromptsEnabled(false),
            AutofillSettingsAction.SetUnmatchedSuggestionsEnabled(true),
            AutofillSettingsAction.SetMaxSuggestions(7),
        )
        actions.forEach(model::onAction)
        runCurrent()
        assertEquals(List(actions.size) {
            AutofillSettingsEffect.SaveFailed(UiErrorMessage(R.string.ui_error_file_io))
        }, effects)
        assertEquals(before, model.uiState.value)
        assertEquals(emptyList<Boolean>(), repository.savePromptWrites)
        assertEquals(0, repository.credentialManagerWrites)
    }

    private class RecordingInteractionSettingsRepository(
        override val interaction: kotlinx.coroutines.flow.Flow<InteractionSettings> =
            MutableStateFlow(InteractionSettings()),
        private val save: suspend () -> Unit = {},
    ) : InteractionSettingsRepository {
        val savePromptWrites = mutableListOf<Boolean>()
        var credentialManagerWrites = 0

        override suspend fun setAutofillSavePromptsEnabled(enabled: Boolean) {
            save()
            savePromptWrites += enabled
        }

        override suspend fun setCredentialManagerEnabled(enabled: Boolean) {
            save()
            credentialManagerWrites++
        }

        override suspend fun setSwipeEnabled(enabled: Boolean) = Unit
        override suspend fun setPullToSearchEnabled(enabled: Boolean) = Unit
        override suspend fun setSwipeLeftAction(action: SwipeActionType) = Unit
        override suspend fun setSwipeRightAction(action: SwipeActionType) = Unit
        override suspend fun setAutofillEnabled(enabled: Boolean) = save()
        override suspend fun setAutofillPresentation(presentation: AutofillPresentation) = save()
        override suspend fun setAutofillAuthenticationRequired(required: Boolean) = save()
        override suspend fun setAutofillOtpEnabled(enabled: Boolean) = save()
        override suspend fun setUnmatchedAutofillSuggestionsEnabled(enabled: Boolean) = save()
        override suspend fun setAutofillMaxSuggestions(count: Int) = save()
    }
}
