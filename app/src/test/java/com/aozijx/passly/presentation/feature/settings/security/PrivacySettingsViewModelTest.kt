package com.aozijx.passly.presentation.feature.settings.security

import com.aozijx.passly.domain.settings.model.SecuritySettings
import com.aozijx.passly.domain.settings.port.SecuritySettingsRepository
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

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

    private object EmptySecuritySettingsRepository : SecuritySettingsRepository {
        override val security = emptyFlow<SecuritySettings>()
        override suspend fun setSecureContentEnabled(enabled: Boolean) = Unit
        override suspend fun setFlipToLockEnabled(enabled: Boolean) = Unit
        override suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean) = Unit
        override suspend fun setLockOnBackground(enabled: Boolean) = Unit
        override suspend fun setLockTimeout(timeoutMs: Long) = Unit
        override suspend fun setInvalidateBiometricKeyOnChange(enabled: Boolean) = Unit
        override suspend fun setReauthenticateSensitiveCopies(enabled: Boolean) = Unit
        override suspend fun setClipboardClearEnabled(enabled: Boolean) = Unit
        override suspend fun setClipboardClearDelaySeconds(delaySeconds: Int) = Unit
    }
}
