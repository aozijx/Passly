package com.aozijx.passly.presentation.feature.backup.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.feature.backup.ui.model.BackupRestoreSheetUiState
import com.aozijx.passly.presentation.feature.backup.ui.model.BackupSheet
import com.aozijx.passly.presentation.feature.backup.ui.model.BackupSheetEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupRestoreSheet(
    state: BackupRestoreSheetUiState,
    onEvent: (BackupSheetEvent) -> Unit,
) {
    when (state.activeSheet ?: return) {
        BackupSheet.FORMAT_PICKER -> BackupSheetFrame(
            onDismiss = { onEvent(BackupSheetEvent.Dismissed) },
        ) {
            BackupFormatPickerContent { onEvent(BackupSheetEvent.FormatSelected(it)) }
        }
        BackupSheet.EXPORT_OPTIONS -> BackupSheetFrame(
            onDismiss = { onEvent(BackupSheetEvent.Dismissed) },
        ) {
            BackupExportOptionsContent(state, onEvent)
        }
        BackupSheet.IMPORT_OPTIONS -> BackupSheetFrame(
            onDismiss = { onEvent(BackupSheetEvent.Dismissed) },
        ) {
            BackupImportOptionsContent(state, onEvent)
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
