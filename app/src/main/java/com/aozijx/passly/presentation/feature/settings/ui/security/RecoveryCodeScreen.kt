package com.aozijx.passly.presentation.feature.settings.ui.security

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
import com.aozijx.passly.presentation.feature.settings.security.RecoveryCodeDraftStatus
import com.aozijx.passly.presentation.feature.settings.security.RecoveryCodeSettingsAction
import com.aozijx.passly.presentation.feature.settings.security.RecoveryCodeSettingsUiState
import com.aozijx.passly.presentation.feature.settings.ui.main.SettingsSecondaryPage
import com.aozijx.passly.presentation.feature.settings.ui.main.component.SettingsGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryCodeScreen(
    state: RecoveryCodeSettingsUiState,
    onAction: (RecoveryCodeSettingsAction) -> Unit,
    onBack: (() -> Unit)?,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val errorMessage = when (state.draftStatus) {
        RecoveryCodeDraftStatus.EXPIRED ->
            stringResource(R.string.settings_recovery_code_draft_expired)
        RecoveryCodeDraftStatus.FAILED ->
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

    if (state.draftStatus == RecoveryCodeDraftStatus.READY) {
        RecoveryCodeSheet(
            recoveryCode = state.disclosure.toUiString(),
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
