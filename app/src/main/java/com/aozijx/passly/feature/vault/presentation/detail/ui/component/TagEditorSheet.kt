package com.aozijx.passly.feature.vault.presentation.detail.ui.component

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.autofill.excludeFromAutofill
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailTagChipsUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.TagEditorValidationUiState
import com.aozijx.passly.feature.vault.presentation.detail.DetailTagEditorState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun TagEditorSheet(
    state: DetailTagEditorState.Open,
    isSaving: Boolean,
    onInputChanged: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onRemove: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
) {
    val dismissAllowed by rememberUpdatedState(!isSaving)
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            confirmValueChange = { target -> target != SheetValue.Hidden || dismissAllowed },
        )
    val scope = rememberCoroutineScope()
    val keepEditing: () -> Unit = {
        onKeepEditing()
        scope.launch { sheetState.show() }
        Unit
    }
    ModalBottomSheet(
        onDismissRequest = { if (dismissAllowed) onDismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.vault_detail_tags_title),
                style = MaterialTheme.typography.headlineSmall,
            )

            when (val draftTags = state.draftTagsUiState) {
                DetailTagChipsUiState.Empty -> Unit
                is DetailTagChipsUiState.Items -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        draftTags.tags.forEach { tag ->
                            val removeLabel = stringResource(R.string.vault_detail_tag_remove, tag)
                            InputChip(
                                enabled = !isSaving,
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
                                modifier =
                                    Modifier.semantics {
                                        if (!isSaving) {
                                            customActions =
                                                listOf(
                                                    CustomAccessibilityAction(removeLabel) {
                                                        onRemove(tag)
                                                        true
                                                    },
                                                )
                                        }
                                    },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                enabled = !isSaving,
                value = state.input,
                onValueChange = onInputChanged,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("tag-editor-input")
                        .excludeFromAutofill(),
                label = { Text(stringResource(R.string.vault_detail_tag_input_label)) },
                placeholder = { Text(stringResource(R.string.vault_detail_tag_input_hint)) },
                supportingText =
                    {
                        when (val validation = state.validation) {
                            TagEditorValidationUiState.None -> Unit
                            is TagEditorValidationUiState.Invalid ->
                                Text(
                                    text =
                                        pluralStringResource(
                                            validation.pluralResourceId,
                                            validation.quantity,
                                            validation.quantity,
                                        ),
                                )
                        }
                    },
                isError = state.validation is TagEditorValidationUiState.Invalid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions =
                    KeyboardActions(
                        onDone = { onSubmit(state.input) },
                    ),
            )

            when (val suggestions = state.suggestionsUiState) {
                DetailTagChipsUiState.Empty -> Unit
                is DetailTagChipsUiState.Items -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        suggestions.tags.forEach { suggestion ->
                            AssistChip(
                                enabled = !isSaving,
                                onClick = { onSubmit(suggestion) },
                                label = { Text(suggestion) },
                            )
                        }
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
                    modifier =
                        Modifier
                            .weight(1f)
                            .testTag("tag-editor-save"),
                ) {
                    if (isSaving) {
                        LoadingIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }

    if (state.showDiscardConfirmation && !isSaving) {
        AlertDialog(
            onDismissRequest = keepEditing,
            title = { Text(stringResource(R.string.vault_detail_tag_discard_title)) },
            text = { Text(stringResource(R.string.vault_detail_tag_discard_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmDiscard) {
                    Text(stringResource(R.string.vault_detail_tag_discard_action))
                }
            },
            dismissButton = {
                TextButton(onClick = keepEditing) {
                    Text(stringResource(R.string.vault_detail_tag_keep_editing))
                }
            },
        )
    }
}
