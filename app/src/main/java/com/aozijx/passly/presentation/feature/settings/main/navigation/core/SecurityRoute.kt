package com.aozijx.passly.presentation.feature.settings.main.navigation.core

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.settings.main.SettingsEffect
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiAction
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.buildAppPasswordDialogEventHandler
import com.aozijx.passly.presentation.feature.settings.main.buildAppPasswordDialogsModel
import com.aozijx.passly.presentation.feature.settings.main.navigation.toMessage
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordAction
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsAction
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsViewModel
import com.aozijx.passly.presentation.feature.settings.security.toSecuritySettingsUiModel
import com.aozijx.passly.presentation.feature.settings.security.validateAndSendAppPasswordAction
import com.aozijx.passly.presentation.ui.settings.main.AppPasswordDialogs
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import com.aozijx.passly.presentation.ui.settings.main.rememberAppPasswordDialogStateHolder
import com.aozijx.passly.presentation.ui.settings.security.SecurityDetail

@Composable
internal fun SecurityRoute(
    onBack: (() -> Unit)?,
) {
    val context = LocalContext.current
    val securityViewModel: SecuritySettingsViewModel = hiltViewModel()
    val appPasswordViewModel: SettingsViewModel = hiltViewModel()
    val securityState by securityViewModel.uiState.collectAsStateWithLifecycle()
    val appPasswordState by appPasswordViewModel.uiState.collectAsStateWithLifecycle()
    val appPasswordDialogs = rememberAppPasswordDialogStateHolder()

    fun submitAppPasswordAction(action: AppPasswordAction) {
        validateAndSendAppPasswordAction(
            context = context,
            action = action,
            currentPassword = appPasswordDialogs.appPasswordCurrent,
            newPassword = appPasswordDialogs.appPasswordNew,
            confirmPassword = appPasswordDialogs.appPasswordConfirm,
            settingsViewModel = appPasswordViewModel,
        )
    }

    LaunchedEffect(appPasswordViewModel, context) {
        appPasswordViewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.AppPasswordSet,
                SettingsEffect.AppPasswordChanged,
                SettingsEffect.AppPasswordDisabled -> appPasswordDialogs.onAppPasswordSuccess()
                is SettingsEffect.AppPasswordEntryAuthorized -> {
                    if (effect.alreadyEnabled) {
                        appPasswordDialogs.openAppPasswordActionDialog()
                    } else {
                        appPasswordDialogs.openSetAppPasswordDialog()
                    }
                }
                else -> Unit
            }
            effect.toMessage(context)?.let { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.SECURITY.titleRes),
        onBack = onBack,
    ) {
        item {
            SecurityDetail(
                state = securityState.toSecuritySettingsUiModel(
                    isAppPasswordEnabled = appPasswordState.isAppPasswordEnabled,
                ),
                onLockTimeoutChange = {
                    securityViewModel.onAction(SecuritySettingsAction.SetLockTimeout(it))
                },
                onAppPasswordClick = {
                    appPasswordViewModel.onAction(SettingsUiAction.RequestAppPasswordEntry)
                },
                onBiometricEnabledChange = { enabled ->
                    securityViewModel.onAction(
                        SecuritySettingsAction.SetBiometricEnabled(enabled),
                    )
                },
                onInvalidateKeyOnBioChangeToggle = { enabled ->
                    securityViewModel.onAction(
                        SecuritySettingsAction.SetInvalidateKeyOnBiometricChange(enabled),
                    )
                },
                onLockOnBackgroundChange = {
                    securityViewModel.onAction(SecuritySettingsAction.ToggleLockOnBackground(it))
                },
            )
        }
    }

    AppPasswordDialogs(
        state = buildAppPasswordDialogsModel(appPasswordDialogs),
        onEvent = buildAppPasswordDialogEventHandler(
            stateHolder = appPasswordDialogs,
            submitAppPasswordAction = ::submitAppPasswordAction,
        ),
    )
}