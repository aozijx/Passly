package com.aozijx.passly.presentation.feature.settings.main.navigation.core

import com.aozijx.passly.presentation.feature.settings.main.navigation.SettingsRoute
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.appearance.AppearanceSettingsAction
import com.aozijx.passly.presentation.feature.settings.appearance.AppearanceSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.appearance.InterfaceSettingsAction
import com.aozijx.passly.presentation.feature.settings.appearance.InterfaceSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.appearance.toInterfaceUiModel
import com.aozijx.passly.presentation.feature.settings.appearance.libraryQuickFilterOptions
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiAction
import com.aozijx.passly.presentation.feature.settings.security.PrivacySettingsAction
import com.aozijx.passly.presentation.feature.settings.security.PrivacySettingsViewModel
import com.aozijx.passly.presentation.feature.settings.security.PrivacySettingsEffect
import com.aozijx.passly.presentation.feature.settings.security.toPrivacySettingsUiModel
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsAction
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsViewModel
import com.aozijx.passly.presentation.feature.settings.security.toSecuritySettingsUiModel
import com.aozijx.passly.presentation.feature.settings.appearance.appLanguageFromKey
import com.aozijx.passly.presentation.feature.settings.appearance.toAppearanceUiModel
import com.aozijx.passly.presentation.feature.settings.appearance.toDomainModel
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import com.aozijx.passly.presentation.ui.settings.security.PrivacyDetail
import com.aozijx.passly.presentation.ui.settings.security.SecurityDetail
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage
import com.aozijx.passly.presentation.ui.settings.appearance.AppearanceDetail
import com.aozijx.passly.presentation.ui.settings.appearance.InterfaceDetail
import com.aozijx.passly.presentation.ui.settings.appearance.LibraryQuickFiltersSettingsSection

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
