package com.aozijx.passly.feature.settings.presentation.main.navigation.core

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.feature.settings.presentation.common.ObserveSettingsWriteErrors
import com.aozijx.passly.feature.settings.presentation.security.AppPasswordDialogCoordinatorOwner
import com.aozijx.passly.feature.settings.presentation.security.AppPasswordSettingsAction
import com.aozijx.passly.feature.settings.presentation.security.AppPasswordSettingsEffect
import com.aozijx.passly.feature.settings.presentation.security.AppPasswordSettingsViewModel
import com.aozijx.passly.feature.settings.presentation.security.SecuritySettingsAction
import com.aozijx.passly.feature.settings.presentation.security.SecuritySettingsNotice
import com.aozijx.passly.feature.settings.presentation.security.SecuritySettingsViewModel
import com.aozijx.passly.feature.settings.presentation.security.rememberAppPasswordDialogCoordinator
import com.aozijx.passly.feature.settings.presentation.security.toAppPasswordMessage
import com.aozijx.passly.feature.settings.presentation.security.toSecuritySettingsUiModel
import com.aozijx.passly.feature.settings.presentation.ui.main.SettingsSecondaryPage
import com.aozijx.passly.feature.settings.presentation.ui.main.component.SettingsGroup
import com.aozijx.passly.feature.settings.presentation.ui.security.AppPasswordDialogs
import com.aozijx.passly.feature.settings.presentation.ui.security.SecurityDetail
import com.aozijx.passly.presentation.shared.event.collectPageEffects
import com.aozijx.passly.presentation.shared.state.SettingsReadFailureContent

@Composable
internal fun SecurityRoute(onBack: (() -> Unit)?) {
    val context = LocalContext.current
    val biometricCleanupPendingMessage =
        stringResource(com.aozijx.passly.R.string.settings_biometric_cleanup_pending)
    val authenticationAvailabilityRefreshPendingMessage =
        stringResource(com.aozijx.passly.R.string.settings_biometric_availability_refresh_pending)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val securityViewModel: SecuritySettingsViewModel = hiltViewModel()
    val appPasswordViewModel: AppPasswordSettingsViewModel = hiltViewModel()
    val appPasswordDialogs = rememberAppPasswordDialogCoordinator()
    AppPasswordDialogCoordinatorOwner(appPasswordDialogs)
    DisposableEffect(securityViewModel, appPasswordViewModel) {
        val securityOwner = securityViewModel.onPageEntered()
        val appPasswordOwner = appPasswordViewModel.onPageEntered()
        onDispose {
            securityViewModel.onPageExited(securityOwner)
            appPasswordViewModel.onPageExited(appPasswordOwner)
        }
    }
    ObserveSettingsWriteErrors(securityViewModel.errors)
    LaunchedEffect(securityViewModel, lifecycle, context, biometricCleanupPendingMessage, authenticationAvailabilityRefreshPendingMessage) {
        collectPageEffects(lifecycle, securityViewModel.notices) { notice ->
            val message =
                when (notice) {
                    SecuritySettingsNotice.BiometricRotationCleanupPending -> {
                        biometricCleanupPendingMessage
                    }

                    SecuritySettingsNotice.AuthenticationAvailabilityRefreshPending -> {
                        authenticationAvailabilityRefreshPendingMessage
                    }
                }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    val securityState by securityViewModel.uiState.collectAsStateWithLifecycle()
    val appPasswordState by appPasswordViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(appPasswordViewModel, lifecycle, context) {
        collectPageEffects(lifecycle, appPasswordViewModel.effects) { effect ->
            when (effect) {
                AppPasswordSettingsEffect.AppPasswordSet,
                AppPasswordSettingsEffect.AppPasswordChanged,
                AppPasswordSettingsEffect.AppPasswordDisabled,
                -> {
                    appPasswordDialogs.onAppPasswordOperationSucceeded()
                }

                is AppPasswordSettingsEffect.AppPasswordEntryAuthorized -> {
                    appPasswordDialogs.onAppPasswordEntryAuthorized(effect.alreadyEnabled)
                }

                else -> {
                    Unit
                }
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
        when (val state = securityState) {
            RecoverableReadState.Loading -> {
                Unit
            }

            is RecoverableReadState.Failed -> {
                item { SettingsReadFailureContent(onRetry = { securityViewModel.onAction(SecuritySettingsAction.RetryRead) }) }
            }

            is RecoverableReadState.Ready -> {
                item {
                    SecurityDetail(
                        state =
                            state.value.toSecuritySettingsUiModel(
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
        }
    }

    AppPasswordDialogs(
        state =
            appPasswordDialogs.model.copy(
                isBusy = appPasswordState.isOperationInProgress,
            ),
        onEvent = { event ->
            appPasswordDialogs
                .onEvent(
                    event = event,
                    isBusy = appPasswordState.isOperationInProgress,
                )?.let { request ->
                    appPasswordViewModel.onAction(
                        AppPasswordSettingsAction.SubmitChange(request),
                    )
                }
        },
    )
}
