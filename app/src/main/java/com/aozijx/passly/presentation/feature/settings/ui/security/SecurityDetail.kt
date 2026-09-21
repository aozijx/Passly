package com.aozijx.passly.presentation.feature.settings.ui.security

import androidx.compose.runtime.Composable
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.presentation.feature.settings.ui.security.model.SecuritySettingsUiModel

@Composable
internal fun SecurityDetail(
    state: SecuritySettingsUiModel,
    onLockTimeoutChange: (Long) -> Unit,
    onAppPasswordClick: () -> Unit,
    onBiometricEnabledChange: (Boolean) -> Unit,
    onInvalidateKeyOnBioChangeToggle: (Boolean) -> Unit,
    onLockOnBackgroundChange: (Boolean) -> Unit
) {
    SettingsSection {
        LockAuthSettingsSection(
            state = state,
            onLockTimeoutChange = onLockTimeoutChange,
            onAppPasswordClick = onAppPasswordClick,
            onBiometricEnabledChange = onBiometricEnabledChange,
            onInvalidateKeyOnBioChangeToggle = onInvalidateKeyOnBioChangeToggle,
            onLockOnBackgroundChange = onLockOnBackgroundChange
        )
    }
}
