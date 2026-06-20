package com.aozijx.passly.ui.features.settings.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.ui.features.settings.shell.sectionSpacing

@Composable
internal fun PrivacyDetail(
    state: PrivacyUiState,
    onSecureContentEnabledChange: (Boolean) -> Unit,
    onFlipToLockEnabledChange: (Boolean) -> Unit,
    onFlipExitAndClearStackEnabledChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.sectionSpacing()) {
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