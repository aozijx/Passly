package com.aozijx.passly.presentation.ui.settings.backup

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.ui.settings.backup.model.BackupRestoreSheetEventHandler
import com.aozijx.passly.presentation.ui.settings.backup.model.BackupRestoreSheetUiState
import com.aozijx.passly.presentation.ui.settings.backup.model.BackupSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupRestoreSheet(
    state: BackupRestoreSheetUiState,
    eventHandler: BackupRestoreSheetEventHandler,
) {
    when (state.activeSheet ?: return) {
        BackupSheet.FORMAT_PICKER -> BackupSheetFrame(eventHandler::onDismiss) {
            BackupFormatPickerContent(eventHandler::onFormatSelected)
        }
        BackupSheet.EXPORT_OPTIONS -> BackupSheetFrame(eventHandler::onDismiss) {
            BackupExportOptionsContent(state, eventHandler)
        }
        BackupSheet.IMPORT_OPTIONS -> BackupSheetFrame(eventHandler::onDismiss) {
            BackupImportOptionsContent(state, eventHandler)
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
