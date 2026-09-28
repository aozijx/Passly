package com.aozijx.passly.presentation.feature.backup.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.feature.backup.BackupOptionsStage
import com.aozijx.passly.presentation.feature.backup.BackupUiAction
import com.aozijx.passly.presentation.feature.backup.ui.model.BackupRestoreSheetUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupRestoreSheet(
    state: BackupRestoreSheetUiState,
    onAction: (BackupUiAction) -> Unit,
) {
    when (state.activeSheet ?: return) {
        BackupOptionsStage.FORMAT_PICKER -> BackupSheetFrame(
            onDismiss = { onAction(BackupUiAction.DismissOptions) },
        ) {
            BackupFormatPickerContent { onAction(BackupUiAction.SelectExportFormat(it)) }
        }
        BackupOptionsStage.EXPORT_OPTIONS -> BackupSheetFrame(
            onDismiss = { onAction(BackupUiAction.DismissOptions) },
        ) {
            BackupExportOptionsContent(state, onAction)
        }
        BackupOptionsStage.IMPORT_OPTIONS -> BackupSheetFrame(
            onDismiss = { onAction(BackupUiAction.DismissOptions) },
        ) {
            BackupImportOptionsContent(state, onAction)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupSheetFrame(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        content = { content() },
    )
}
