package com.aozijx.passly.ui.features.settings.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.ui.features.settings.shell.sectionSpacing

@Composable
internal fun SecurityDetail(
    state: SecurityUiState,
    isAppPasswordEnabled: Boolean,
    onLockTimeoutChange: (Long) -> Unit,
    onAppPasswordClick: () -> Unit,
    onInvalidateKeyOnBioChangeToggle: (Boolean) -> Unit,
    onLockOnBackgroundChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.sectionSpacing()) {
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
    }
}