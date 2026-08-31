package com.aozijx.passly.presentation.ui.vault.detail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailTagEditorUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.TagEditorValidationErrorUiModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagEditorSheet(
    state: DetailTagEditorUiModel,
    isSaving: Boolean,
    onInputChanged: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onRemove: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.vault_detail_tags_title),
                style = MaterialTheme.typography.headlineSmall,
            )

            if (state.draftTags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.draftTags.forEach { tag ->
                        val removeLabel = stringResource(R.string.vault_detail_tag_remove, tag)
                        InputChip(
                            selected = true,
                            onClick = { onRemove(tag) },
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = removeLabel,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            modifier = Modifier.semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction(removeLabel) {
                                        onRemove(tag)
                                        true
                                    },
                                )
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.input,
                onValueChange = onInputChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tag-editor-input"),
                label = { Text(stringResource(R.string.vault_detail_tag_input_label)) },
                placeholder = { Text(stringResource(R.string.vault_detail_tag_input_hint)) },
                supportingText = state.validationError?.let { error ->
                    {
                        Text(
                            text = when (error) {
                                TagEditorValidationErrorUiModel.TOO_MANY_TAGS -> stringResource(
                                    R.string.vault_detail_tag_too_many,
                                    20,
                                )
                                TagEditorValidationErrorUiModel.TAG_TOO_LONG -> stringResource(
                                    R.string.vault_detail_tag_too_long,
                                    32,
                                )
                            },
                        )
                    }
                },
                isError = state.validationError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (state.input.isNotBlank()) onSubmit(state.input) },
                ),
            )

            if (state.suggestions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.suggestions.forEach { suggestion ->
                        AssistChip(
                            onClick = { onSubmit(suggestion) },
                            label = { Text(suggestion) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = onSave,
                    enabled = state.dirty && !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tag-editor-save"),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }

    if (state.confirmDiscard) {
        AlertDialog(
            onDismissRequest = onKeepEditing,
            title = { Text(stringResource(R.string.vault_detail_tag_discard_title)) },
            text = { Text(stringResource(R.string.vault_detail_tag_discard_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmDiscard) {
                    Text(stringResource(R.string.vault_detail_tag_discard_action))
                }
            },
            dismissButton = {
                TextButton(onClick = onKeepEditing) {
                    Text(stringResource(R.string.vault_detail_tag_keep_editing))
                }
            },
        )
    }
}
