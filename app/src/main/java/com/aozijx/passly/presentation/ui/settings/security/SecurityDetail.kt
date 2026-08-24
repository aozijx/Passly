package com.aozijx.passly.presentation.ui.settings.security

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.presentation.ui.settings.security.model.SecuritySettingsUiModel

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
        Spacer(modifier = Modifier.height(8.dp))
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
