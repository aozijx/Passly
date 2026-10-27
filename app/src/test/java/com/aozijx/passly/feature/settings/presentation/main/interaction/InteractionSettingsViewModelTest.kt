package com.aozijx.passly.feature.settings.presentation.main.interaction

import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.port.InteractionSettingsRepository
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
        override val interaction = flowOf(InteractionSettings())
        override suspend fun setSwipeLeftAction(action: SwipeActionType) = save()
        override suspend fun setSwipeRightAction(action: SwipeActionType) = save()
        override suspend fun setSwipeEnabled(enabled: Boolean) = Unit
        override suspend fun setPullToSearchEnabled(enabled: Boolean) = Unit
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
