package com.aozijx.passly.feature.settings.presentation.security

import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import com.aozijx.passly.domain.settings.model.SecuritySettings
import com.aozijx.passly.domain.settings.port.SecuritySettingsRepository
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacySettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `remains loading until persisted security preferences arrive`() = runTest {
        val viewModel = PrivacySettingsViewModel(
            settingsRepository = EmptySecuritySettingsRepository,
            clipboardCleaner = { false },
        )

        assertEquals(PrivacySettingsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `all privacy preference failures retain source state and report errors`() = runTest {
        val repository = object : SecuritySettingsRepository by EmptySecuritySettingsRepository {
            override val security = MutableStateFlow(SecuritySettings())
            private fun fail(): Nothing = throw IOException("synthetic private diagnostic")
            override suspend fun setSecureContentEnabled(enabled: Boolean) = fail()
            override suspend fun setFlipToLockEnabled(enabled: Boolean) = fail()
            override suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean) = fail()
            override suspend fun setReauthenticateSensitiveCopies(enabled: Boolean) = fail()
            override suspend fun setClipboardClearEnabled(enabled: Boolean) = fail()
            override suspend fun setClipboardClearDelaySeconds(delaySeconds: Int) = fail()
        }
        var clipboardCalls = 0
        val model = PrivacySettingsViewModel(repository) { clipboardCalls++; false }
        val effects = mutableListOf<PrivacySettingsEffect>()
        backgroundScope.launch { model.effects.toList(effects) }
        backgroundScope.launch { model.uiState.collect {} }
        runCurrent()
        val before = model.uiState.value
        val actions = listOf(
            PrivacySettingsAction.SetSecureContentEnabled(false),
            PrivacySettingsAction.SetFlipToLockEnabled(true),
            PrivacySettingsAction.SetFlipExitAndClearStackEnabled(true),
            PrivacySettingsAction.SetSensitiveCopyReauthentication(false),
            PrivacySettingsAction.SetClipboardClearEnabled(false),
            PrivacySettingsAction.SetClipboardClearDelaySeconds(15),
        )
        actions.forEach(model::onAction)
        runCurrent()
        assertEquals(List(actions.size) {
            PrivacySettingsEffect.SaveFailed(UiErrorMessage(R.string.ui_error_file_io))
        }, effects)
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
