package com.aozijx.passly.feature.settings.presentation.main.interaction

import androidx.lifecycle.ViewModelStore
import com.aozijx.passly.R
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.port.InteractionSettingsRepository
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InteractionSettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `preference read failure is visible and explicit retry re-subscribes`() =
        runTest {
            var starts = 0
            val repository =
                Repository(
                    save = {},
                    source =
                        flow {
                            starts++
                            if (starts == 1) throw IOException("read failure")
                            emit(InteractionSettings())
                            awaitCancellation()
                        },
                )
            val model = InteractionSettingsViewModel(repository)
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()

            assertEquals(true, model.uiState.value is RecoverableReadState.Failed)
            model.onAction(InteractionSettingsAction.RetryRead)
            runCurrent()

            assertEquals(2, starts)
            assertEquals(true, model.uiState.value is RecoverableReadState.Ready<*>)
        }

    @Test
    fun `toggle failures are reported while repository remains the state source`() =
        runTest {
            val repository = Repository { throw IOException("synthetic write failure") }
            val model = InteractionSettingsViewModel(repository)
            model.onPageEntered()
            val effects = mutableListOf<InteractionSettingsEffect>()
            backgroundScope.launch { model.effects.toList(effects) }
            backgroundScope.launch { model.uiState.toList() }
            runCurrent()
            model.onAction(InteractionSettingsAction.SetSwipeEnabled(true))
            model.onAction(InteractionSettingsAction.SetPullToSearchEnabled(true))
            runCurrent()
            val error = InteractionSettingsEffect.SaveFailed(UiErrorMessage(R.string.ui_error_file_io))
            assertEquals(listOf(error, error), effects)
            val ready = (model.uiState.value as RecoverableReadState.Ready<InteractionSettings>).value
            assertEquals(false, ready.isSwipeEnabled)
            assertEquals(false, ready.isPullToSearchEnabled)
        }

    @Test
    fun `successful toggles stay silent and follow repository emissions`() =
        runTest {
            val repository = Repository { }
            val model = InteractionSettingsViewModel(repository)
            model.onPageEntered()
            val effects = mutableListOf<InteractionSettingsEffect>()
            backgroundScope.launch { model.effects.toList(effects) }
            backgroundScope.launch { model.uiState.toList() }
            runCurrent()
            model.onAction(InteractionSettingsAction.SetSwipeEnabled(true))
            model.onAction(InteractionSettingsAction.SetPullToSearchEnabled(true))
            runCurrent()
            val ready = (model.uiState.value as RecoverableReadState.Ready<InteractionSettings>).value
            assertEquals(true, ready.isSwipeEnabled)
            assertEquals(true, ready.isPullToSearchEnabled)
            assertEquals(emptyList<InteractionSettingsEffect>(), effects)
            repository.currentInteraction.value = InteractionSettings()
            runCurrent()
            assertEquals(
                false,
                (model.uiState.value as RecoverableReadState.Ready<InteractionSettings>).value.isSwipeEnabled,
            )
        }

    @Test
    fun `cancelled slow writes cannot publish late success or failure`() =
        runTest {
            for (failsAfterCancellation in listOf(false, true)) {
                val release = CompletableDeferred<Unit>()
                val repository =
                    Repository {
                        withContext(NonCancellable) { release.await() }
                        if (failsAfterCancellation) throw IOException("synthetic late failure")
                    }
                val model = InteractionSettingsViewModel(repository)
                model.onPageEntered()
                val effects = mutableListOf<InteractionSettingsEffect>()
                backgroundScope.launch { model.effects.toList(effects) }
                model.onAction(InteractionSettingsAction.SetSwipeLeftAction(SwipeActionType.DELETE))
                runCurrent()
                ViewModelStore().apply {
                    put("interaction", model)
                    clear()
                }
                release.complete(Unit)
                runCurrent()
                assertEquals(emptyList<InteractionSettingsEffect>(), effects)
            }
        }

    @Test
    fun `cancelled setting write emits no failure notification`() =
        runTest {
            val model = InteractionSettingsViewModel(Repository { throw CancellationException() })
            model.onPageEntered()
            val effects = mutableListOf<InteractionSettingsEffect>()
            backgroundScope.launch { model.effects.toList(effects) }
            model.onAction(InteractionSettingsAction.SetSwipeLeftAction(SwipeActionType.DELETE))
            runCurrent()
            assertEquals(emptyList<InteractionSettingsEffect>(), effects)
        }

    @Test
    fun `storage failure emits resource key without exception contents`() =
        runTest {
            val model = InteractionSettingsViewModel(Repository { throw IOException("synthetic private diagnostic") })
            model.onPageEntered()
            val effects = mutableListOf<InteractionSettingsEffect>()
            backgroundScope.launch { model.effects.toList(effects) }
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()
            model.onAction(InteractionSettingsAction.SetSwipeRightAction(SwipeActionType.DELETE))
            runCurrent()
            assertEquals(listOf(InteractionSettingsEffect.SaveFailed(UiErrorMessage(R.string.ui_error_file_io))), effects)
        }

    @Test
    fun `successful setting write keeps saved feedback`() =
        runTest {
            val model = InteractionSettingsViewModel(Repository { })
            model.onPageEntered()
            val effects = mutableListOf<InteractionSettingsEffect>()
            backgroundScope.launch { model.effects.toList(effects) }
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()
            model.onAction(InteractionSettingsAction.SetSwipeLeftAction(SwipeActionType.DELETE))
            runCurrent()
            assertEquals(listOf(InteractionSettingsEffect.Saved), effects)
        }

    private class Repository(
        source: Flow<InteractionSettings>? = null,
        private val save: suspend () -> Unit,
    ) : InteractionSettingsRepository {
        val currentInteraction = MutableStateFlow(InteractionSettings())
        override val interaction: Flow<InteractionSettings> = source ?: currentInteraction

        override suspend fun setSwipeLeftAction(action: SwipeActionType) = save()

        override suspend fun setSwipeRightAction(action: SwipeActionType) = save()

        override suspend fun setSwipeEnabled(enabled: Boolean) {
            save()
            currentInteraction.value = currentInteraction.value.copy(isSwipeEnabled = enabled)
        }

        override suspend fun setPullToSearchEnabled(enabled: Boolean) {
            save()
            currentInteraction.value = currentInteraction.value.copy(isPullToSearchEnabled = enabled)
        }

    }
}
