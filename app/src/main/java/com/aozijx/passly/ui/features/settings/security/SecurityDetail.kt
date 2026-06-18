package com.aozijx.passly.ui.features.settings.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.ui.features.settings.internal.SettingsContentActions
import com.aozijx.passly.ui.features.settings.internal.SettingsContentState
import com.aozijx.passly.ui.features.settings.shell.sectionSpacing

@Composable
internal fun SecurityDetail(
    state: SettingsContentState,
    actions: SettingsContentActions
) {
    Column(modifier = Modifier.sectionSpacing()) {
        Spacer(modifier = Modifier.height(8.dp))
        LockAuthSettingsSection(
            lockTimeout = state.lockTimeout,
            isAppPasswordEnabled = state.isAppPasswordEnabled,
            isPasswordPreferredAuthFirst = state.isPasswordPreferredAuthFirst,
            isDeviceCredentialFallbackEnabled = state.isDeviceCredentialFallbackEnabled,
            isInvalidateKeyOnBioChange = state.isInvalidateKeyOnBioChange,
            onLockTimeoutChange = actions.onLockTimeoutChange,
            onAppPasswordClick = actions.onAppPasswordClick,
            onPasswordPreferredAuthFirstChange = actions.onPasswordPreferredAuthFirstChange,
            onDeviceCredentialFallbackToggleRequested = actions.onDeviceCredentialFallbackToggleRequested,
            onInvalidateKeyOnBioChangeToggle = actions.onInvalidateKeyOnBioChangeToggle
        )
    }
}