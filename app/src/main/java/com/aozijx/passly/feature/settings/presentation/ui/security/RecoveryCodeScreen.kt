package com.aozijx.passly.feature.settings.presentation.ui.security

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.settings.presentation.security.RecoveryCodeDraftUiState
import com.aozijx.passly.feature.settings.presentation.security.RecoveryCodeSettingsAction
import com.aozijx.passly.feature.settings.presentation.security.RecoveryCodeSettingsUiState
import com.aozijx.passly.feature.settings.presentation.ui.main.SettingsSecondaryPage
import com.aozijx.passly.feature.settings.presentation.ui.main.component.SettingsGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryCodeScreen(
    state: RecoveryCodeSettingsUiState,
    onAction: (RecoveryCodeSettingsAction) -> Unit,
    onBack: (() -> Unit)?,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val errorMessage = when (state.draft) {
        RecoveryCodeDraftUiState.Expired ->
            stringResource(R.string.settings_recovery_code_draft_expired)
        RecoveryCodeDraftUiState.Failed ->
            stringResource(R.string.settings_recovery_code_operation_failed)
        else -> null
    }

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.RECOVERY_CODE.titleRes),
        onBack = onBack,
    ) {
        errorMessage?.let { message ->
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            RecoveryCodeDetail(state = state, onAction = onAction)
        }
    }

    (state.draft as? RecoveryCodeDraftUiState.Ready)?.let { ready ->
        RecoveryCodeSheet(
            recoveryCode = ready.disclosure.toUiString(),
            sheetState = sheetState,
            onCopy = { onAction(RecoveryCodeSettingsAction.Copy) },
            onConfirm = { onAction(RecoveryCodeSettingsAction.ConfirmAndEnable) },
            onDismiss = { onAction(RecoveryCodeSettingsAction.DismissDisclosure) },
        )
    }
}

private fun SensitiveValue.toUiString(): String {
    val chars = toCharArray()
    return try {
        String(chars)
    } finally {
        MemoryCleaner.wipeCharArray(chars)
    }
}
