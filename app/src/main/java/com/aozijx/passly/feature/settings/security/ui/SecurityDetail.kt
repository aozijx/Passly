package com.aozijx.passly.feature.settings.security.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.feature.settings.security.SecurityUiState
import com.aozijx.passly.ui.components.settings.SettingsSection

@Composable
internal fun SecurityDetail(
    state: SecurityUiState,
    isAppPasswordEnabled: Boolean,
    onLockTimeoutChange: (Long) -> Unit,
    onAppPasswordClick: () -> Unit,
    onInvalidateKeyOnBioChangeToggle: (Boolean) -> Unit,
    onLockOnBackgroundChange: (Boolean) -> Unit,
    onClipboardClearToastsChange: (Boolean) -> Unit,
    onAppCloseToastsChange: (Boolean) -> Unit
) {
    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))
        LockAuthSettingsSection(
            lockTimeout = state.lockTimeout,
            isAppPasswordEnabled = isAppPasswordEnabled,
            isInvalidateKeyOnBioChange = state.isInvalidateKeyOnBioChange,
            isLockOnBackground = state.isLockOnBackground,
            onLockTimeoutChange = onLockTimeoutChange,
            onAppPasswordClick = onAppPasswordClick,
            onInvalidateKeyOnBioChangeToggle = onInvalidateKeyOnBioChangeToggle,
            onLockOnBackgroundChange = onLockOnBackgroundChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        SecurityToastSettingsSection(
            clipboardClearEnabled = state.clipboardClearToastsEnabled,
            appCloseEnabled = state.appCloseToastsEnabled,
            onClipboardClearEnabledChange = onClipboardClearToastsChange,
            onAppCloseEnabledChange = onAppCloseToastsChange
        )
    }
}
