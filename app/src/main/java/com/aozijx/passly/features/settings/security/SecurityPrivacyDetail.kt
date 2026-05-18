package com.aozijx.passly.features.settings.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.features.settings.shell.SettingsContentActions
import com.aozijx.passly.features.settings.shell.SettingsContentState
import com.aozijx.passly.features.settings.shell.sectionSpacing

@Composable
internal fun SecurityPrivacyDetail(
    state: SettingsContentState,
    actions: SettingsContentActions
) {
    Column(modifier = Modifier.sectionSpacing()) {
        Spacer(modifier = Modifier.height(8.dp))
        SecurityPrivacySettingsSection(
            lockTimeout = state.lockTimeout,
            isAppPasswordEnabled = state.isAppPasswordEnabled,
            isPasswordPreferredAuthFirst = state.isPasswordPreferredAuthFirst,
            isDeviceCredentialFallbackEnabled = state.isDeviceCredentialFallbackEnabled,
            isInvalidateKeyOnBioChange = state.isInvalidateKeyOnBioChange,
            isSecureContentEnabled = state.isSecureContentEnabled,
            isFlipToLockEnabled = state.isFlipToLockEnabled,
            isFlipExitAndClearStackEnabled = state.isFlipExitAndClearStackEnabled,
            onLockTimeoutClick = actions.onShowLockTimeoutDialog,
            onAppPasswordClick = actions.onAppPasswordClick,
            onPasswordPreferredAuthFirstChange = actions.onPasswordPreferredAuthFirstChange,
            onDeviceCredentialFallbackToggleRequested = actions.onDeviceCredentialFallbackToggleRequested,
            onInvalidateKeyOnBioChangeToggle = actions.onInvalidateKeyOnBioChangeToggle,
            onSecureContentEnabledChange = actions.onSecureContentEnabledChange,
            onFlipToLockEnabledChange = actions.onFlipToLockEnabledChange,
            onFlipExitAndClearStackEnabledChange = actions.onFlipExitAndClearStackEnabledChange
        )
    }
}