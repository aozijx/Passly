package com.aozijx.passly.feature.settings.security.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.feature.settings.security.SecurityUiState

@Composable
internal fun SecurityDetail(
    state: SecurityUiState,
    isAppPasswordEnabled: Boolean,
    isBiometricEnabled: Boolean,
    onLockTimeoutChange: (Long) -> Unit,
    onAppPasswordClick: () -> Unit,
    onBiometricEnabledChange: (Boolean) -> Unit,
    onInvalidateKeyOnBioChangeToggle: (Boolean) -> Unit,
    onLockOnBackgroundChange: (Boolean) -> Unit,
    onSensitiveCopyReauthenticationChange: (Boolean) -> Unit
) {
    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))
        LockAuthSettingsSection(
            lockTimeout = state.lockTimeout,
            isAppPasswordEnabled = isAppPasswordEnabled,
            isBiometricEnabled = isBiometricEnabled,
            isInvalidateKeyOnBioChange = state.isInvalidateKeyOnBioChange,
            isLockOnBackground = state.isLockOnBackground,
            reauthenticateSensitiveCopies = state.reauthenticateSensitiveCopies,
            onLockTimeoutChange = onLockTimeoutChange,
            onAppPasswordClick = onAppPasswordClick,
            onBiometricEnabledChange = onBiometricEnabledChange,
            onInvalidateKeyOnBioChangeToggle = onInvalidateKeyOnBioChangeToggle,
            onLockOnBackgroundChange = onLockOnBackgroundChange,
            onSensitiveCopyReauthenticationChange =
                onSensitiveCopyReauthenticationChange
        )
    }
}
