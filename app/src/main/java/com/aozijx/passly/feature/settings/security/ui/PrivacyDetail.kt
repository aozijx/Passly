package com.aozijx.passly.feature.settings.security.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.feature.settings.security.PrivacyUiState
import com.aozijx.passly.ui.components.settings.SettingsSection

@Composable
internal fun PrivacyDetail(
    state: PrivacyUiState,
    onSecureContentEnabledChange: (Boolean) -> Unit,
    onFlipToLockEnabledChange: (Boolean) -> Unit,
    onFlipExitAndClearStackEnabledChange: (Boolean) -> Unit
) {
    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))
        SecurityProtectionSettingsSection(
            isSecureContentEnabled = state.isSecureContentEnabled,
            isFlipToLockEnabled = state.isFlipToLockEnabled,
            isFlipExitAndClearStackEnabled = state.isFlipExitAndClearStackEnabled,
            onSecureContentEnabledChange = onSecureContentEnabledChange,
            onFlipToLockEnabledChange = onFlipToLockEnabledChange,
            onFlipExitAndClearStackEnabledChange = onFlipExitAndClearStackEnabledChange
        )
    }
}
