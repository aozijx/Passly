package com.aozijx.passly.feature.vault.components.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.aozijx.passly.feature.vault.editor.common.EntryEditorSection
import com.aozijx.passly.feature.vault.editor.common.EntryNotesField
import com.aozijx.passly.feature.vault.editor.common.EntryPasswordField
import com.aozijx.passly.feature.vault.editor.common.EntrySecretField
import com.aozijx.passly.feature.vault.editor.common.EntryTitleField
import com.aozijx.passly.feature.vault.editor.common.EntryUsernameField

@Composable
fun DynamicEntryEditorForm(
    schema: EntryEditorSchema,
    state: EntryEditorFormState,
    onValueChange: () -> Unit
) {
    schema.sections.forEach { section ->
        EntryEditorSection(title = stringResource(section.titleRes)) {
            section.fields.forEach { field ->
                EntryEditorField(
                    field = field,
                    state = state,
                    onValueChange = onValueChange
                )
            }
        }
    }
}

@Composable
private fun EntryEditorField(
    field: EntryEditorFieldSchema,
    state: EntryEditorFormState,
    onValueChange: () -> Unit
) {
    val value = state.value(field.key)
    val label = stringResource(field.labelRes)
    val update: (String) -> Unit = {
        state.update(field.key, it)
        onValueChange()
    }

    when (field.kind) {
        EntryEditorFieldKind.Password -> EntryPasswordField(
            password = value,
            onPasswordChange = update,
            isVisible = state.isSecretVisible,
            onVisibilityChange = { state.isSecretVisible = it },
            label = label,
            imeAction = ImeAction.Next
        )

        EntryEditorFieldKind.Notes -> EntryNotesField(
            value = value,
            onValueChange = update,
            label = label
        )

        EntryEditorFieldKind.Text -> when (field.key) {
            EntryEditorFieldKey.TITLE -> EntryTitleField(
                value = value,
                onValueChange = update,
                label = label
            )

            EntryEditorFieldKey.SUMMARY -> EntryUsernameField(
                value = value,
                onValueChange = update,
                label = label
            )

            else -> EntrySecretField(
                value = value,
                onValueChange = update,
                label = label,
                keyboardType = field.keyboardType,
                singleLine = field.singleLine,
                imeAction = ImeAction.Next
            )
        }
    }
}
