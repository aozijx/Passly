package com.aozijx.passly.presentation.feature.settings.main.navigation.core

import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.settings.security.RecoveryDraftAction
import com.aozijx.passly.presentation.feature.settings.security.RecoveryDraftEffect
import com.aozijx.passly.presentation.feature.settings.security.RecoveryDraftState
import com.aozijx.passly.presentation.feature.settings.security.RecoveryDraftViewModel
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsAction
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsViewModel
import com.aozijx.passly.presentation.feature.settings.security.messageOrNull
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import com.aozijx.passly.presentation.ui.settings.security.RecoveryCodeDetail
import com.aozijx.passly.presentation.ui.settings.security.RecoveryCodeSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecoveryCodeRoute(
    onBack: (() -> Unit)?,
) {
    val context = LocalContext.current
    val viewModel: SecuritySettingsViewModel = hiltViewModel()
    val draftViewModel: RecoveryDraftViewModel = hiltViewModel()
    val draftState by draftViewModel.state.collectAsStateWithLifecycle()
    val securityState by viewModel.uiState.collectAsStateWithLifecycle()
    var showRecoveryCodeSheet by rememberSaveable { mutableStateOf(false) }
    val recoveryCodeSheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val copySuccessMessage = stringResource(
        R.string.field_copy_success_message,
        stringResource(R.string.recovery_code_label),
    )

    LaunchedEffect(draftViewModel, context, copySuccessMessage) {
        draftViewModel.effects.collect { effect ->
            when (effect) {
                RecoveryDraftEffect.Copied -> Toast.makeText(
                    context,
                    copySuccessMessage,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val recoveryCode = remember(draftState) {
        if (draftState is RecoveryDraftState.Ready) {
            draftViewModel.revealCode()?.concatToString()
        } else {
            null
        }
    }
    LaunchedEffect(recoveryCode) {
        showRecoveryCodeSheet = recoveryCode != null
    }
    recoveryCode?.let { code ->
        if (showRecoveryCodeSheet) {
            RecoveryCodeSheet(
                recoveryCode = code,
                sheetState = recoveryCodeSheetState,
                onCopy = { draftViewModel.onAction(RecoveryDraftAction.Copy) },
                onConfirm = {
                    showRecoveryCodeSheet = false
                    draftViewModel.onAction(RecoveryDraftAction.ConfirmAndEnable)
                },
                onDismiss = {
                    showRecoveryCodeSheet = false
                    draftViewModel.onAction(RecoveryDraftAction.Dismiss)
                },
            )
        }
    }

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.RECOVERY_CODE.titleRes),
        onBack = onBack,
    ) {
        draftState.messageOrNull()?.let { message ->
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
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
                },
            )
        }
    }
}