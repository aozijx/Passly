package com.aozijx.passly.feature.settings.presentation.autofill

import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.port.InteractionSettingsRepository
import com.aozijx.passly.feature.autofill.platform.AutofillServiceStatusSource
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutofillSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `remains loading until persisted preferences arrive`() =
        runTest {
            val repository = RecordingInteractionSettingsRepository(emptyFlow())
            val viewModel =
                AutofillSettingsViewModel(
                    settingsRepository = repository,
                    autofillServiceStatusSource = AutofillServiceStatusSource { MutableStateFlow(false) },
                )
            viewModel.onPageEntered()

            assertEquals(
                AutofillSourceState.Loading<AutofillSettings>(),
                viewModel.uiState.value.preferences,
            )
        }

    @Test
    fun `preference failure does not expose stale settings and does not fail system status`() =
        runTest {
            val lastSettings = AutofillSettings(enabled = false)
            val repository =
                RecordingInteractionSettingsRepository(
                    interaction =
                        flow {
                            emit(InteractionSettings(autofill = lastSettings))
                            throw IOException("preferences unavailable")
                        },
                )
            val viewModel =
                AutofillSettingsViewModel(
                    repository,
                    AutofillServiceStatusSource { MutableStateFlow(true) },
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            assertEquals(
                AutofillSourceState.Failed(null),
                viewModel.uiState.value.preferences,
            )
            assertEquals(AutofillSourceState.Ready(true), viewModel.uiState.value.systemService)
        }

    @Test
    fun `system status failure is not presented as disabled`() =
        runTest {
            val repository = RecordingInteractionSettingsRepository()
            val viewModel =
                AutofillSettingsViewModel(
                    repository,
                    AutofillServiceStatusSource { flow { throw IOException("status unavailable") } },
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            assertEquals(
                AutofillSourceState.Ready(AutofillSettings()),
                viewModel.uiState.value.preferences,
            )
            assertEquals(
                AutofillSourceState.Failed<Boolean>(null),
                viewModel.uiState.value.systemService,
            )
        }

    @Test
    fun `retry restarts only failed system source`() =
        runTest {
            var preferenceStarts = 0
            var systemStarts = 0
            var cancelledSystemCollectors = 0
            val repository =
                RecordingInteractionSettingsRepository(
                    interaction =
                        flow {
                            preferenceStarts++
                            emit(InteractionSettings())
                            kotlinx.coroutines.awaitCancellation()
                        },
                )
            val viewModel =
                AutofillSettingsViewModel(
                    repository,
                    AutofillServiceStatusSource {
                        flow {
                            systemStarts++
                            try {
                                if (systemStarts == 1) {
                                    emit(false)
                                    throw IOException("status unavailable")
                                }
                                emit(true)
                                kotlinx.coroutines.awaitCancellation()
                            } finally {
                                cancelledSystemCollectors++
                            }
                        }
                    },
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()
            assertEquals(1, preferenceStarts)
            assertEquals(1, systemStarts)
            assertEquals(AutofillSourceState.Failed(false), viewModel.uiState.value.systemService)

            viewModel.onAction(AutofillSettingsAction.RetrySystemService)
            runCurrent()

            assertEquals(1, preferenceStarts)
            assertEquals(2, systemStarts)
            assertEquals(1, cancelledSystemCollectors)
            assertEquals(AutofillSourceState.Ready(true), viewModel.uiState.value.systemService)
        }

    @Test
    fun `retrying failed preferences keeps system collector and recovers controls`() =
        runTest {
            var preferenceStarts = 0
            var systemStarts = 0
            val repository =
                RecordingInteractionSettingsRepository(
                    interaction =
                        flow {
                            preferenceStarts++
                            if (preferenceStarts == 1) throw IOException("preferences unavailable")
                            emit(InteractionSettings(autofill = AutofillSettings(enabled = false)))
                            kotlinx.coroutines.awaitCancellation()
                        },
                )
            val viewModel =
                AutofillSettingsViewModel(
                    repository,
                    AutofillServiceStatusSource {
                        flow {
                            systemStarts++
                            emit(true)
                            kotlinx.coroutines.awaitCancellation()
                        }
                    },
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()
            assertEquals(AutofillSourceState.Failed<AutofillSettings>(null), viewModel.uiState.value.preferences)
            assertEquals(1, systemStarts)
            viewModel.onAction(AutofillSettingsAction.SetSavePromptsEnabled(false))
            runCurrent()
            assertEquals(emptyList<Boolean>(), repository.savePromptWrites)

            viewModel.onAction(AutofillSettingsAction.RetryPreferences)
            runCurrent()

            assertEquals(2, preferenceStarts)
            assertEquals(1, systemStarts)
            assertEquals(
                AutofillSourceState.Ready(AutofillSettings(enabled = false)),
                viewModel.uiState.value.preferences,
            )
            assertEquals(AutofillSourceState.Ready(true), viewModel.uiState.value.systemService)
        }

    @Test
    fun `source collector is cancelled after the page loses its state subscriber`() =
        runTest {
            var sourceCancelled = false
            val viewModel =
                AutofillSettingsViewModel(
                    RecordingInteractionSettingsRepository(),
                    AutofillServiceStatusSource {
                        flow {
                            emit(true)
                            try {
                                kotlinx.coroutines.awaitCancellation()
                            } finally {
                                sourceCancelled = true
                            }
                        }
                    },
                )
            val collection = backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()
            collection.cancel()
            advanceTimeBy(5_001L)
            runCurrent()

            assertEquals(true, sourceCancelled)
        }

    @Test
    fun `save prompt action updates only the legacy save preference`() =
        runTest {
            val repository = RecordingInteractionSettingsRepository()
            val viewModel =
                AutofillSettingsViewModel(
                    settingsRepository = repository,
                    autofillServiceStatusSource = AutofillServiceStatusSource { MutableStateFlow(false) },
                )
            viewModel.onPageEntered()
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            viewModel.onAction(AutofillSettingsAction.SetSavePromptsEnabled(false))
            advanceUntilIdle()

            assertEquals(listOf(false), repository.savePromptWrites)
            assertEquals(0, repository.credentialManagerWrites)
        }

    @Test
    fun `strict domain matching action updates only that preference`() =
        runTest {
            val repository = RecordingInteractionSettingsRepository()
            val viewModel =
                AutofillSettingsViewModel(
                    settingsRepository = repository,
                    autofillServiceStatusSource = AutofillServiceStatusSource { MutableStateFlow(false) },
                )
            viewModel.onPageEntered()
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            viewModel.onAction(AutofillSettingsAction.SetStrictDomainMatching(false))
            advanceUntilIdle()

            assertEquals(listOf(false), repository.strictDomainMatchingWrites)
            assertEquals(emptyList<Boolean>(), repository.savePromptWrites)
            assertEquals(0, repository.credentialManagerWrites)
        }

    @Test
    fun `all autofill preference failures are reported without changing source state or opening settings`() =
        runTest {
            val repository = RecordingInteractionSettingsRepository(save = { throw IOException("synthetic failure") })
            val model = AutofillSettingsViewModel(repository, AutofillServiceStatusSource { MutableStateFlow(false) })
            model.onPageEntered()
            val effects = mutableListOf<AutofillSettingsEffect>()
            backgroundScope.launch { model.effects.toList(effects) }
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()
            val before = model.uiState.value
            val actions =
                listOf(
                    AutofillSettingsAction.SetEnabled(false),
                    AutofillSettingsAction.SetPresentation(AutofillPresentation.entries.first()),
                    AutofillSettingsAction.SetCredentialManagerEnabled(false),
                    AutofillSettingsAction.SetAuthenticationRequired(false),
                    AutofillSettingsAction.SetOtpEnabled(false),
                    AutofillSettingsAction.SetSavePromptsEnabled(false),
                    AutofillSettingsAction.SetUnmatchedSuggestionsEnabled(true),
                    AutofillSettingsAction.SetStrictDomainMatching(false),
                    AutofillSettingsAction.SetMaxSuggestions(7),
                )
            actions.forEach(model::onAction)
            runCurrent()
            assertEquals(
                List(actions.size) {
                    AutofillSettingsEffect.SaveFailed(UiErrorMessage(R.string.ui_error_file_io))
                },
                effects,
            )
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
        val strictDomainMatchingWrites = mutableListOf<Boolean>()

        override suspend fun setAutofillSavePromptsEnabled(enabled: Boolean) {
            save()
            savePromptWrites += enabled
        }

        override suspend fun setCredentialManagerEnabled(enabled: Boolean) {
            save()
            credentialManagerWrites++
        }

        override suspend fun setAutofillStrictDomainMatching(enabled: Boolean) {
            save()
            strictDomainMatchingWrites += enabled
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
