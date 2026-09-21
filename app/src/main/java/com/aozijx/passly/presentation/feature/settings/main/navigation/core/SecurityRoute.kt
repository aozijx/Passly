package com.aozijx.passly.presentation.feature.settings.main.navigation.core

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordSettingsEffect
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordSettingsAction
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.security.buildAppPasswordDialogEventHandler
import com.aozijx.passly.presentation.feature.settings.security.buildAppPasswordDialogsModel
import com.aozijx.passly.presentation.feature.settings.security.toAppPasswordMessage
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordAction
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsAction
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsViewModel
import com.aozijx.passly.presentation.feature.settings.security.toSecuritySettingsUiModel
import com.aozijx.passly.presentation.feature.settings.security.validateAndSendAppPasswordAction
import com.aozijx.passly.presentation.feature.settings.ui.main.AppPasswordDialogs
import com.aozijx.passly.presentation.feature.settings.ui.main.SettingsSecondaryPage
import com.aozijx.passly.presentation.feature.settings.ui.main.component.SettingsGroup
import com.aozijx.passly.presentation.feature.settings.ui.main.rememberAppPasswordDialogStateHolder
import com.aozijx.passly.presentation.feature.settings.ui.security.SecurityDetail

@Composable
internal fun SecurityRoute(
    onBack: (() -> Unit)?,
) {
    val context = LocalContext.current
    val securityViewModel: SecuritySettingsViewModel = hiltViewModel()
    val appPasswordViewModel: AppPasswordSettingsViewModel = hiltViewModel()
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
                AppPasswordSettingsEffect.AppPasswordSet,
                AppPasswordSettingsEffect.AppPasswordChanged,
                AppPasswordSettingsEffect.AppPasswordDisabled -> appPasswordDialogs.onAppPasswordSuccess()
                is AppPasswordSettingsEffect.AppPasswordEntryAuthorized -> {
                    if (effect.alreadyEnabled) {
                        appPasswordDialogs.openAppPasswordActionDialog()
                    } else {
                        appPasswordDialogs.openSetAppPasswordDialog()
                    }
                }
                else -> Unit
            }
            effect.toAppPasswordMessage(context)?.let { message ->
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
                    appPasswordViewModel.onAction(AppPasswordSettingsAction.RequestAppPasswordEntry)
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