package com.aozijx.passly.presentation.feature.settings.main.navigation.core

import com.aozijx.passly.presentation.feature.settings.main.navigation.SettingsRoute
import android.content.Context
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiState
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.security.RecoveryDraftAction
import com.aozijx.passly.presentation.feature.settings.security.RecoveryDraftState
import com.aozijx.passly.presentation.feature.settings.security.RecoveryDraftViewModel
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsAction
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsViewModel
import com.aozijx.passly.presentation.feature.settings.security.messageOrNull
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import com.aozijx.passly.presentation.ui.settings.security.RecoveryCodeDetail
import com.aozijx.passly.presentation.ui.settings.security.RecoveryCodeSheet
import com.aozijx.passly.presentation.ui.settings.main.SettingsScreenLocalState
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecoveryCodeRouteContent(
    route: SettingsRoute,
    context: Context,
    localState: SettingsScreenLocalState,
    interactionViewModel: InteractionSettingsViewModel,
    dataViewModel: DataManagementSettingsViewModel,
    settingsViewModel: SettingsViewModel,
    settingsState: SettingsUiState,
    onBack: (() -> Unit)?
) {
    val viewModel: SecuritySettingsViewModel = hiltViewModel()
    val draftViewModel: RecoveryDraftViewModel = hiltViewModel()
    val draftState by draftViewModel.state.collectAsStateWithLifecycle()
    val securityState by viewModel.uiState.collectAsStateWithLifecycle()
    val recoveryCode = remember(draftState) {
        if (draftState is RecoveryDraftState.Ready) {
            draftViewModel.revealCode()?.concatToString()
        } else {
            null
        }
    }
    LaunchedEffect(recoveryCode) {
        if (recoveryCode != null) localState.showRecoveryCodeSheet = true
    }
    recoveryCode?.let { code ->
        if (localState.showRecoveryCodeSheet) {
            val copySuccessMessage = stringResource(
                R.string.field_copy_success_message,
                stringResource(R.string.recovery_code_label),
            )
            RecoveryCodeSheet(
                recoveryCode = code,
                sheetState = localState.recoveryCodeSheetState,
                onCopy = {
                    settingsViewModel.copySensitive(code)
                    Toast.makeText(
                        context,
                        copySuccessMessage,
                        Toast.LENGTH_SHORT,
                    ).show()
                },
                onConfirm = {
                    localState.showRecoveryCodeSheet = false
                    draftViewModel.onAction(RecoveryDraftAction.ConfirmAndEnable)
                },
                onDismiss = {
                    localState.showRecoveryCodeSheet = false
                    draftViewModel.onAction(RecoveryDraftAction.Dismiss)
                }
            )
        }
    }

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.RECOVERY_CODE.titleRes),
        onBack = onBack
    ) {
        draftState.messageOrNull()?.let { message ->
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            RecoveryCodeDetail(
                hasRecoveryEnvelope = securityState.hasRecoveryEnvelope ||
                        draftState is RecoveryDraftState.Committed,
                verifyResult = securityState.recoveryCodeVerificationResult,
                onCreateRecoveryCode = {
                    draftViewModel.onAction(RecoveryDraftAction.Generate)
                },
                onRegenerate = {
                    draftViewModel.onAction(RecoveryDraftAction.Generate)
                },
                onVerifyCode = {
                    viewModel.onAction(SecuritySettingsAction.VerifyRecoveryCode(it))
                },
                onClearVerifyResult = {
                    viewModel.onAction(SecuritySettingsAction.ClearVerifyResult)
                }
            )
        }
    }
}
