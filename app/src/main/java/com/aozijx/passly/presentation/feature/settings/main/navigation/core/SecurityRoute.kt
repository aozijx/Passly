package com.aozijx.passly.presentation.feature.settings.main.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiAction
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsAction
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsViewModel
import com.aozijx.passly.presentation.feature.settings.security.toSecuritySettingsUiModel
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import com.aozijx.passly.presentation.ui.settings.security.SecurityDetail
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage

@Composable
internal fun SecurityRouteContent(
    settingsViewModel: SettingsViewModel,
    onBack: (() -> Unit)?
) {
    val viewModel: SecuritySettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.SECURITY.titleRes),
        onBack = onBack
    ) {
        item {
            SecurityDetail(
                state = state.toSecuritySettingsUiModel(
                    isAppPasswordEnabled = settingsState.isAppPasswordEnabled,
                ),
                onLockTimeoutChange = {
                    viewModel.onAction(SecuritySettingsAction.SetLockTimeout(it))
                },
                onAppPasswordClick = {
                    settingsViewModel.onAction(SettingsUiAction.RequestAppPasswordEntry)
                },
                onBiometricEnabledChange = { enabled ->
                    viewModel.onAction(
                        SecuritySettingsAction.SetBiometricEnabled(enabled)
                    )
                },
                onInvalidateKeyOnBioChangeToggle = { enabled ->
                    viewModel.onAction(
                        SecuritySettingsAction.SetInvalidateKeyOnBiometricChange(enabled)
                    )
                },
                onLockOnBackgroundChange = {
                    viewModel.onAction(SecuritySettingsAction.ToggleLockOnBackground(it))
                }
            )
        }
    }
}
