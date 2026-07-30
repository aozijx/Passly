package com.aozijx.passly.feature.vault.components.editor

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.ui.components.AppDialog
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.feature.vault.model.AddType

@Composable
fun AddEntryDialog(
    viewModel: VaultViewModel,
    addType: AddType,
    onUpdateInteraction: () -> Unit
) {
    val context = LocalContext.current
    val schema = remember(addType) { addType.toEntryEditorSchema() }
    val state = remember(addType) { EntryEditorFormState() }
    val typeLabel = stringResource(schema.titleRes)
    val saveFailedMessage = stringResource(R.string.vault_add_entry_save_failed)

    AppDialog(
        title = stringResource(R.string.vault_add_generic_title, typeLabel),
        onDismiss = { viewModel.setAddType(null) },
        confirmEnabled = state.canSave,
        onConfirm = {
            try {
                viewModel.addItem(schema.toVaultEntry(state))
                viewModel.setAddType(null)
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
