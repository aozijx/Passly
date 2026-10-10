package com.aozijx.passly.presentation.feature.vault.editor.ui.wifi

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.presentation.feature.vault.editor.ui.common.AddEntryScaffold
import com.aozijx.passly.presentation.feature.vault.editor.ui.common.EntryEditorSection
import com.aozijx.passly.presentation.feature.vault.editor.wifi.AddWifiAction
import com.aozijx.passly.presentation.feature.vault.editor.wifi.AddWifiSavePhase
import com.aozijx.passly.presentation.feature.vault.editor.wifi.AddWifiUiState
import com.aozijx.passly.presentation.shared.components.NextFocusTextField

@Composable
fun AddWifiEditorScreen(
    state: AddWifiUiState,
    onAction: (AddWifiAction) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    saveActionModifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val form = state.form

    fun save() {
        keyboardController?.hide()
        onAction(AddWifiAction.Save)
    }

    AddEntryScaffold(
        title = stringResource(R.string.vault_add_wifi_title),
        canSave = state.canSave,
        isSaving = state.savePhase == AddWifiSavePhase.Saving,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSave = ::save,
        modifier = modifier,
        saveActionModifier = saveActionModifier,
    ) {
        EntryEditorSection(title = stringResource(R.string.vault_editor_section_basic_info)) {
            NextFocusTextField(
                value = form.title,
                onValueChange = { onAction(AddWifiAction.TitleChanged(it)) },
                label = stringResource(R.string.field_title),
                isError = FieldKey.TITLE in state.fieldErrors,
            )
            NextFocusTextField(
                value = form.ssid,
                onValueChange = { onAction(AddWifiAction.SsidChanged(it)) },
                label = stringResource(R.string.wifi_ssid),
                isError = FieldKey.WIFI_SSID in state.fieldErrors,
                supportingText = if (FieldKey.WIFI_SSID in state.fieldErrors) {
                    { Text(stringResource(R.string.wifi_ssid_required)) }
                } else {
                    null
                },
            )
        }

        WifiFormFields(
            form = form,
            fieldErrors = state.fieldErrors,
            onAction = onAction,
        )

        EntryEditorSection(title = stringResource(R.string.vault_editor_section_details)) {
            NextFocusTextField(
                value = form.tags,
                onValueChange = { onAction(AddWifiAction.TagsChanged(it)) },
                label = stringResource(R.string.field_category),
            )
            OutlinedTextField(
                value = form.notes,
                onValueChange = { onAction(AddWifiAction.NotesChanged(it)) },
                label = { Text(stringResource(R.string.field_notes)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (state.canSave) {
                            focusManager.clearFocus(force = true)
                            save()
                        }
                    },
                ),
            )
        }
    }
}
