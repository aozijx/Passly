package com.aozijx.passly.feature.vault.components.editor

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.ui.components.dialog.PasslyAlertDialog
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.feature.vault.model.AddType

@Composable
fun AddEntryDialog(
    addType: AddType,
    onAddItem: (Entry) -> Unit,
    onDismiss: () -> Unit,
    onUpdateInteraction: () -> Unit
) {
    val context = LocalContext.current
    val schema = remember(addType) { addType.toEntryEditorSchema() }
    val state = remember(addType) { EntryEditorFormState() }
    val typeLabel = stringResource(schema.titleRes)
    val saveFailedMessage = stringResource(R.string.vault_add_entry_save_failed)

    PasslyAlertDialog(
        title = stringResource(R.string.vault_add_generic_title, typeLabel),
        confirmText = stringResource(R.string.save),
        dismissText = stringResource(R.string.cancel),
        onDismiss = onDismiss,
        confirmEnabled = state.canSave,
        onConfirm = {
            try {
                onAddItem(schema.toEntry(state))
                onDismiss()
            } catch (e: Exception) {
                AppTelemetry.e("AddEntryDialog", "Failed to save", e)
                Toast.makeText(context, saveFailedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        DynamicEntryEditorForm(
            schema = schema,
            state = state,
            onValueChange = onUpdateInteraction
        )
    }
}
