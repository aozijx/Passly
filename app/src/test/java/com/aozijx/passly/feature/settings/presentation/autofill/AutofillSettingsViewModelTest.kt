package com.aozijx.passly.feature.settings.presentation.autofill

import com.aozijx.passly.R
import com.aozijx.passly.app.autofill.AutofillServiceStatusSource
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.port.AutofillSettingsRepository
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
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
            val repository = RecordingAutofillSettingsRepository(emptyFlow())
            val viewModel =
                AutofillSettingsViewModel(
                    settingsRepository = repository,
                    autofillServiceStatusSource = AutofillServiceStatusSource { MutableStateFlow(false) },
                )
            viewModel.onPageEntered()

            assertEquals(
                AutofillPreferencesState.Loading,
                viewModel.uiState.value.preferences,
            )
        }

    @Test
    fun `preference failure does not expose stale settings and does not fail system status`() =
        runTest {
            val lastSettings = autofillSettings(enabled = false)
            val repository =
                RecordingAutofillSettingsRepository(
                    autofillSettings =
                        flow {
                            emit(lastSettings)
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
                AutofillPreferencesState.Failed,
                viewModel.uiState.value.preferences,
            )
            assertEquals(AutofillSystemServiceStatus.Available(true), viewModel.uiState.value.systemServiceStatus)
        }

    @Test
    fun `system status failure is not presented as disabled`() =
        runTest {
            val repository = RecordingAutofillSettingsRepository()
            val viewModel =
                AutofillSettingsViewModel(
                    repository,
                    AutofillServiceStatusSource { flow { throw IOException("status unavailable") } },
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            assertEquals(
                AutofillPreferencesState.Ready(autofillSettings()),
                viewModel.uiState.value.preferences,
            )
            assertEquals(
                AutofillSystemServiceStatus.Unavailable(null),
                viewModel.uiState.value.systemServiceStatus,
            )
        }

    @Test
    fun `retry restarts only failed system source`() =
        runTest {
            var preferenceStarts = 0
            var systemStarts = 0
            var cancelledSystemCollectors = 0
            val retryGate = CompletableDeferred<Boolean>()
            val repository =
                RecordingAutofillSettingsRepository(
                    autofillSettings =
                        flow {
                            preferenceStarts++
                            emit(autofillSettings())
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
                                emit(retryGate.await())
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
            assertEquals(AutofillSystemServiceStatus.Unavailable(false), viewModel.uiState.value.systemServiceStatus)

            viewModel.onAction(AutofillSettingsAction.RetrySystemService)
            runCurrent()

            assertEquals(1, preferenceStarts)
            assertEquals(2, systemStarts)
            assertEquals(1, cancelledSystemCollectors)
            assertEquals(
                AutofillSystemServiceStatus.Refreshing(false),
                viewModel.uiState.value.systemServiceStatus,
            )

            retryGate.complete(true)
            runCurrent()

            assertEquals(AutofillSystemServiceStatus.Available(true), viewModel.uiState.value.systemServiceStatus)
        }

    @Test
    fun `retrying failed preferences keeps system collector and recovers controls`() =
        runTest {
            var preferenceStarts = 0
            var systemStarts = 0
            val repository =
                RecordingAutofillSettingsRepository(
                    autofillSettings =
                        flow {
                            preferenceStarts++
                            if (preferenceStarts == 1) throw IOException("preferences unavailable")
                            emit(autofillSettings(enabled = false))
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
            assertEquals(AutofillPreferencesState.Failed, viewModel.uiState.value.preferences)
            assertEquals(1, systemStarts)
            viewModel.onAction(AutofillSettingsAction.RetryPreferences)
            runCurrent()

            assertEquals(2, preferenceStarts)
            assertEquals(1, systemStarts)
            assertEquals(
                AutofillPreferencesState.Ready(autofillSettings(enabled = false)),
                viewModel.uiState.value.preferences,
            )
            assertEquals(AutofillSystemServiceStatus.Available(true), viewModel.uiState.value.systemServiceStatus)
        }

    @Test
    fun `source collector is cancelled after the page loses its state subscriber`() =
        runTest {
            var sourceCancelled = false
            val viewModel =
                AutofillSettingsViewModel(
                    RecordingAutofillSettingsRepository(),
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
    fun `presentation and strict matching actions reach their repository writers`() =
        runTest {
            val repository = RecordingAutofillSettingsRepository()
            val viewModel =
                AutofillSettingsViewModel(
                    settingsRepository = repository,
                    autofillServiceStatusSource = AutofillServiceStatusSource { MutableStateFlow(false) },
                )
            viewModel.onPageEntered()
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            viewModel.onAction(AutofillSettingsAction.SetPresentation(AutofillPresentation.BOTTOM_SHEET))
            viewModel.onAction(AutofillSettingsAction.SetStrictDomainMatching(false))
            advanceUntilIdle()

            assertEquals(listOf(AutofillPresentation.BOTTOM_SHEET), repository.presentationWrites)
            assertEquals(listOf(false), repository.strictDomainMatchingWrites)
            assertEquals(0, repository.credentialManagerWrites)
        }

    @Test
    fun `all autofill preference failures are reported without changing source state or opening settings`() =
        runTest {
            val repository = RecordingAutofillSettingsRepository(save = { throw IOException("synthetic failure") })
            val model = AutofillSettingsViewModel(repository, AutofillServiceStatusSource { MutableStateFlow(false) })
            model.onPageEntered()
            val effects = mutableListOf<AutofillSettingsEffect>()
            backgroundScope.launch { model.effects.collect { effects += it } }
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()
            val before = model.uiState.value
            val actions =
                listOf(
                    AutofillSettingsAction.SetEnabled(false),
                    AutofillSettingsAction.SetPresentation(AutofillPresentation.entries.first()),
                    AutofillSettingsAction.SetCredentialManagerEnabled(false),
                    AutofillSettingsAction.SetAuthenticationRequired(false),
                    AutofillSettingsAction.SetStrictDomainMatching(false),
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
            assertEquals(0, repository.credentialManagerWrites)
        }

    private class RecordingAutofillSettingsRepository(
        override val autofillSettings: kotlinx.coroutines.flow.Flow<AutofillSettings> =
            MutableStateFlow(autofillSettings()),
        private val save: suspend () -> Unit = {},
    ) : AutofillSettingsRepository {
        var credentialManagerWrites = 0
        val presentationWrites = mutableListOf<AutofillPresentation>()
        val strictDomainMatchingWrites = mutableListOf<Boolean>()

        override suspend fun setCredentialManagerEnabled(enabled: Boolean) {
            save()
            credentialManagerWrites++
        }

        override suspend fun setAutofillStrictDomainMatching(enabled: Boolean) {
            save()
            strictDomainMatchingWrites += enabled
        }

        override suspend fun setAutofillEnabled(enabled: Boolean) = save()

        override suspend fun setAutofillPresentation(presentation: AutofillPresentation) {
            save()
            presentationWrites += presentation
        }

        override suspend fun setAutofillAuthenticationRequired(required: Boolean) = save()
    }

    private companion object {
        fun autofillSettings(enabled: Boolean = true) =
            AutofillSettings(
                enabled = enabled,
                presentation = AutofillPresentation.FLOATING,
                credentialManagerEnabled = true,
                requireAuthentication = true,
                strictDomainMatching = true,
            )
    }
}
