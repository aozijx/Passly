package com.aozijx.passly.feature.settings.presentation.main.interaction

import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.port.InteractionSettingsRepository
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.testing.MainDispatcherRule
import androidx.lifecycle.ViewModelStore
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InteractionSettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `toggle failures are reported while repository remains the state source`() = runTest {
        val repository = Repository { throw IOException("synthetic write failure") }
        val model = InteractionSettingsViewModel(repository)
        val effects = mutableListOf<InteractionSettingsEffect>()
        backgroundScope.launch { model.effects.toList(effects) }
        backgroundScope.launch { model.uiState.toList() }
        runCurrent()
        model.onAction(InteractionSettingsAction.SetSwipeEnabled(true))
        model.onAction(InteractionSettingsAction.SetPullToSearchEnabled(true))
        runCurrent()
        val error = InteractionSettingsEffect.SaveFailed(UiErrorMessage(R.string.ui_error_file_io))
        assertEquals(listOf(error, error), effects)
        val ready = model.uiState.value as InteractionSettingsUiState.Ready
        assertEquals(false, ready.isSwipeEnabled)
        assertEquals(false, ready.isPullToSearchEnabled)
    }

    @Test
    fun `successful toggles stay silent and follow repository emissions`() = runTest {
        val repository = Repository { }
        val model = InteractionSettingsViewModel(repository)
        val effects = mutableListOf<InteractionSettingsEffect>()
        backgroundScope.launch { model.effects.toList(effects) }
        backgroundScope.launch { model.uiState.toList() }
        runCurrent()
        model.onAction(InteractionSettingsAction.SetSwipeEnabled(true))
        model.onAction(InteractionSettingsAction.SetPullToSearchEnabled(true))
        runCurrent()
        val ready = model.uiState.value as InteractionSettingsUiState.Ready
        assertEquals(true, ready.isSwipeEnabled)
        assertEquals(true, ready.isPullToSearchEnabled)
        assertEquals(emptyList<InteractionSettingsEffect>(), effects)
        repository.interaction.value = InteractionSettings()
        runCurrent()
        assertEquals(false, (model.uiState.value as InteractionSettingsUiState.Ready).isSwipeEnabled)
    }

    @Test
    fun `cancelled slow writes cannot publish late success or failure`() = runTest {
        for (failsAfterCancellation in listOf(false, true)) {
            val release = CompletableDeferred<Unit>()
            val repository = Repository {
                withContext(NonCancellable) { release.await() }
                if (failsAfterCancellation) throw IOException("synthetic late failure")
            }
            val model = InteractionSettingsViewModel(repository)
            val effects = mutableListOf<InteractionSettingsEffect>()
            backgroundScope.launch { model.effects.toList(effects) }
            model.onAction(InteractionSettingsAction.SetSwipeLeftAction(SwipeActionType.DELETE))
            runCurrent()
            ViewModelStore().apply { put("interaction", model); clear() }
            release.complete(Unit)
            runCurrent()
            assertEquals(emptyList<InteractionSettingsEffect>(), effects)
        }
    }

    @Test
    fun `cancelled setting write emits no failure notification`() = runTest {
        val model = InteractionSettingsViewModel(Repository { throw CancellationException() })
        val effects = mutableListOf<InteractionSettingsEffect>()
        backgroundScope.launch { model.effects.toList(effects) }
        model.onAction(InteractionSettingsAction.SetSwipeLeftAction(SwipeActionType.DELETE))
        runCurrent()
        assertEquals(emptyList<InteractionSettingsEffect>(), effects)
    }

    @Test
    fun `storage failure emits resource key without exception contents`() = runTest {
        val model = InteractionSettingsViewModel(Repository { throw IOException("synthetic private diagnostic") })
        val effects = mutableListOf<InteractionSettingsEffect>()
        backgroundScope.launch { model.effects.toList(effects) }
        model.onAction(InteractionSettingsAction.SetSwipeRightAction(SwipeActionType.DELETE))
        runCurrent()
        assertEquals(listOf(InteractionSettingsEffect.SaveFailed(UiErrorMessage(R.string.ui_error_file_io))), effects)
    }

    @Test
    fun `successful setting write keeps saved feedback`() = runTest {
        val model = InteractionSettingsViewModel(Repository { })
        val effects = mutableListOf<InteractionSettingsEffect>()
        backgroundScope.launch { model.effects.toList(effects) }
        model.onAction(InteractionSettingsAction.SetSwipeLeftAction(SwipeActionType.DELETE))
        runCurrent()
        assertEquals(listOf(InteractionSettingsEffect.Saved), effects)
    }

    private class Repository(private val save: suspend () -> Unit) : InteractionSettingsRepository {
        override val interaction = MutableStateFlow(InteractionSettings())
        override suspend fun setSwipeLeftAction(action: SwipeActionType) = save()
        override suspend fun setSwipeRightAction(action: SwipeActionType) = save()
        override suspend fun setSwipeEnabled(enabled: Boolean) {
            save()
            interaction.value = interaction.value.copy(isSwipeEnabled = enabled)
        }
        override suspend fun setPullToSearchEnabled(enabled: Boolean) {
            save()
            interaction.value = interaction.value.copy(isPullToSearchEnabled = enabled)
        }
        override suspend fun setAutofillEnabled(enabled: Boolean) = Unit
        override suspend fun setAutofillPresentation(presentation: AutofillPresentation) = Unit
        override suspend fun setCredentialManagerEnabled(enabled: Boolean) = Unit
        override suspend fun setAutofillAuthenticationRequired(required: Boolean) = Unit
        override suspend fun setAutofillOtpEnabled(enabled: Boolean) = Unit
        override suspend fun setAutofillSavePromptsEnabled(enabled: Boolean) = Unit
        override suspend fun setUnmatchedAutofillSuggestionsEnabled(enabled: Boolean) = Unit
        override suspend fun setAutofillMaxSuggestions(count: Int) = Unit
    }
}
