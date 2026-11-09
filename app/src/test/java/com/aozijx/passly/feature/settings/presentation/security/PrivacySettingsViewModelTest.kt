package com.aozijx.passly.feature.settings.presentation.security

import com.aozijx.passly.R
import com.aozijx.passly.app.settings.ReadFailure
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.settings.model.ClipboardClearPolicy
import com.aozijx.passly.domain.settings.model.SecuritySettings
import com.aozijx.passly.testing.sampleSecuritySettings
import com.aozijx.passly.domain.settings.port.SecuritySettingsRepository
import com.aozijx.passly.feature.settings.presentation.ui.security.model.PrivacySettingsUiModel
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacySettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `security preference failure is visible and retry re-subscribes source`() =
        runTest {
            var starts = 0
            val restoredSettings =
                sampleSecuritySettings().copy(
                    isSecureContentEnabled = false,
                    isFlipToLockEnabled = false,
                    isFlipExitAndClearStackEnabled = true,
                    reauthenticateSensitiveCopies = false,
                    clipboardClearPolicy = ClipboardClearPolicy(enabled = false, delaySeconds = 60),
                )
            val repository =
                object : SecuritySettingsRepository by EmptySecuritySettingsRepository {
                    override val security: Flow<SecuritySettings> =
                        flow {
                            starts++
                            if (starts == 1) throw IOException("read failure")
                            emit(restoredSettings)
                            awaitCancellation()
                        }
                }
            val model = PrivacySettingsViewModel(repository) { false }
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()

            assertEquals(RecoverableReadState.Failed(ReadFailure.IO), model.uiState.value)
            model.onAction(PrivacySettingsAction.RetryRead)
            runCurrent()

            assertEquals(2, starts)
            assertEquals(
                RecoverableReadState.Ready(
                    PrivacySettingsUiModel(
                        isSecureContentEnabled = false,
                        isFlipToLockEnabled = false,
                        isFlipExitAndClearStackEnabled = true,
                        reauthenticateSensitiveCopies = false,
                        clipboardClearEnabled = false,
                        clipboardClearDelaySeconds = 60,
                        clipboardClearDelayOptions = listOf(15, 30, 60, 120),
                    ),
                ),
                model.uiState.value,
            )
        }

    @Test
    fun `remains loading until persisted security preferences arrive`() =
        runTest {
            val viewModel =
                PrivacySettingsViewModel(
                    settingsRepository = EmptySecuritySettingsRepository,
                    clipboardCleaner = { false },
                )

            assertEquals(RecoverableReadState.Loading, viewModel.uiState.value)
        }

    @Test
    fun `clear clipboard maps ownership result to the matching effect`() =
        runTest {
            var ownsClipboard = true
            var clearCalls = 0
            val repository =
                object : SecuritySettingsRepository by EmptySecuritySettingsRepository {
                    override val security = MutableStateFlow(sampleSecuritySettings().copy())
                }
            val model =
                PrivacySettingsViewModel(repository) {
                    clearCalls++
                    ownsClipboard
                }
            model.onPageEntered()
            val effects = mutableListOf<PrivacySettingsEffect>()
            backgroundScope.launch { model.effects.toList(effects) }
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()

            model.onAction(PrivacySettingsAction.ClearClipboardNow)
            runCurrent()
            assertEquals(listOf(PrivacySettingsEffect.ClipboardCleared), effects)

            ownsClipboard = false
            model.onAction(PrivacySettingsAction.ClearClipboardNow)
            runCurrent()

            assertEquals(
                listOf(
                    PrivacySettingsEffect.ClipboardCleared,
                    PrivacySettingsEffect.ClipboardNotCleared,
                ),
                effects,
            )
            assertEquals(2, clearCalls)
        }

    @Test
    fun `all privacy preference failures retain source state and report errors`() =
        runTest {
            val repository =
                object : SecuritySettingsRepository by EmptySecuritySettingsRepository {
                    override val security = MutableStateFlow(sampleSecuritySettings().copy())

                    private fun fail(): Nothing = throw IOException("synthetic private diagnostic")

                    override suspend fun setSecureContentEnabled(enabled: Boolean) = fail()

                    override suspend fun setFlipToLockEnabled(enabled: Boolean) = fail()

                    override suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean) = fail()

                    override suspend fun setReauthenticateSensitiveCopies(enabled: Boolean) = fail()

                    override suspend fun setClipboardClearEnabled(enabled: Boolean) = fail()

                    override suspend fun setClipboardClearDelaySeconds(delaySeconds: Int) = fail()
                }
            var clipboardCalls = 0
            val model =
                PrivacySettingsViewModel(repository) {
                    clipboardCalls++
                    false
                }
            model.onPageEntered()
            val effects = mutableListOf<PrivacySettingsEffect>()
            backgroundScope.launch { model.effects.toList(effects) }
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()
            val before = model.uiState.value
            val actions =
                listOf(
                    PrivacySettingsAction.SetSecureContentEnabled(false),
                    PrivacySettingsAction.SetFlipToLockEnabled(true),
                    PrivacySettingsAction.SetFlipExitAndClearStackEnabled(true),
                    PrivacySettingsAction.SetSensitiveCopyReauthentication(false),
                    PrivacySettingsAction.SetClipboardClearEnabled(false),
                    PrivacySettingsAction.SetClipboardClearDelaySeconds(15),
                )
            actions.forEach(model::onAction)
            runCurrent()
            assertEquals(
                List(actions.size) {
                    PrivacySettingsEffect.SaveFailed(UiErrorMessage(R.string.ui_error_file_io))
                },
                effects,
            )
            assertEquals(before, model.uiState.value)
            assertEquals(0, clipboardCalls)
        }

    private object EmptySecuritySettingsRepository : SecuritySettingsRepository {
        override val security = emptyFlow<SecuritySettings>()

        override suspend fun setSecureContentEnabled(enabled: Boolean) = Unit

        override suspend fun setFlipToLockEnabled(enabled: Boolean) = Unit

        override suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean) = Unit

        override suspend fun setLockOnBackground(enabled: Boolean) = Unit

        override suspend fun setLockTimeout(timeoutMs: Long) = Unit

        override suspend fun setReauthenticateSensitiveCopies(enabled: Boolean) = Unit

        override suspend fun setClipboardClearEnabled(enabled: Boolean) = Unit

        override suspend fun setClipboardClearDelaySeconds(delaySeconds: Int) = Unit
    }
}
