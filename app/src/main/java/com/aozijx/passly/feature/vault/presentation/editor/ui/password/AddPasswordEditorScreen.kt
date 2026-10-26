package com.aozijx.passly.feature.vault.presentation.editor.ui.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.packageinfo.rememberInstalledAppIconBitmap
import com.aozijx.passly.feature.vault.presentation.editor.password.AddPasswordAction
import com.aozijx.passly.feature.vault.presentation.editor.password.AddPasswordSavePhase
import com.aozijx.passly.feature.vault.presentation.editor.password.AddPasswordUiState
import com.aozijx.passly.feature.vault.presentation.editor.ui.common.AddEntryScaffold
import com.aozijx.passly.feature.vault.presentation.editor.ui.common.EntryEditorSection
import com.aozijx.passly.feature.vault.presentation.editor.ui.common.SensitiveFieldVisibilityButton
import com.aozijx.passly.presentation.shared.components.AppPackagePickerBottomSheet
import com.aozijx.passly.presentation.shared.components.NextFocusTextField

@Composable
fun AddPasswordEditorScreen(
    state: AddPasswordUiState,
    onAction: (AddPasswordAction) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    saveActionModifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val form = state.form
    var showAssociatedAppPicker by rememberSaveable { mutableStateOf(false) }
    val selectedApp = form.associatedAppPackage?.let { packageName ->
        state.availableApps.firstOrNull { it.packageName == packageName }
    }

    fun save() {
        keyboardController?.hide()
        onAction(AddPasswordAction.Save)
    }

    AddEntryScaffold(
        title = stringResource(R.string.vault_add_password_title),
        canSave = state.canSave,
        isSaving = state.savePhase == AddPasswordSavePhase.Saving,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSave = ::save,
        modifier = modifier,
        saveActionModifier = saveActionModifier,
    ) {
        EntryEditorSection(title = stringResource(R.string.vault_editor_section_basic_info)) {
            NextFocusTextField(
                value = form.title,
                onValueChange = { onAction(AddPasswordAction.TitleChanged(it)) },
                label = stringResource(R.string.field_title),
            )
            NextFocusTextField(
                value = form.username,
                onValueChange = { onAction(AddPasswordAction.UsernameChanged(it)) },
                label = stringResource(R.string.field_username_hint),
            )
        }

        EntryEditorSection(title = stringResource(R.string.vault_editor_section_credentials)) {
            NextFocusTextField(
                value = form.password,
                onValueChange = { onAction(AddPasswordAction.PasswordChanged(it)) },
                label = stringResource(R.string.password_label),
                visualTransformation = if (form.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                trailingIcon = {
                    SensitiveFieldVisibilityButton(
                        fieldLabelRes = R.string.password_label,
                        visible = form.isPasswordVisible,
                        onVisibilityChanged = { onAction(AddPasswordAction.PasswordVisibilityChanged(it)) },
                    )
                }
            )
        }

        EntryEditorSection(title = stringResource(R.string.vault_editor_section_details)) {
            NextFocusTextField(
                value = form.tags,
                onValueChange = { onAction(AddPasswordAction.TagsChanged(it)) },
                label = stringResource(R.string.field_category),
            )
            NextFocusTextField(
                value = form.websiteUrl,
                onValueChange = { onAction(AddPasswordAction.WebsiteUrlChanged(it)) },
                label = stringResource(R.string.vault_add_password_website_url),
                keyboardType = KeyboardType.Uri,
            )
            OutlinedCard(
                onClick = {
                    showAssociatedAppPicker = true
                    onAction(AddPasswordAction.LoadAssociatedApps)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.vault_detail_associated_package),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = selectedApp?.label
                            ?: form.associatedAppPackage
                            ?: stringResource(R.string.not_set),
                    )
                    selectedApp?.packageName?.let { packageName ->
                        Text(
                            text = packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = form.notes,
                onValueChange = { onAction(AddPasswordAction.NotesChanged(it)) },
                label = { Text(stringResource(R.string.field_notes)) },
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

    if (showAssociatedAppPicker) {
        AppPackagePickerBottomSheet(
            apps = state.availableApps,
            appIcon = { rememberInstalledAppIconBitmap(it) },
            onSelect = { app ->
                onAction(AddPasswordAction.AssociatedAppSelected(app.packageName))
                showAssociatedAppPicker = false
            },
            onDismiss = { showAssociatedAppPicker = false },
        )
    }
}
