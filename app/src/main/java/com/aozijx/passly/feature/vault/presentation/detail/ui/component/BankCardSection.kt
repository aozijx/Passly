package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.autofill.excludeFromAutofill
import com.aozijx.passly.core.ui.components.MaskStyle
import com.aozijx.passly.feature.vault.presentation.detail.DetailField
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBankCardEditorUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBankCardFieldUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBankCardUiModel

@Composable
fun BankCardSection(
    model: DetailBankCardUiModel,
    onAction: (DetailUiAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        model.fields.forEach { field ->
            BankCardField(field, onAction)
        }
        if (model.canRevealMore) {
            Button(
                onClick = { onAction(DetailUiAction.RevealBankCardFields) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vault_reveal_info))
            }
        }
    }
}

@Composable
private fun BankCardField(
    field: DetailBankCardFieldUiModel,
    onAction: (DetailUiAction) -> Unit,
) {
    when (field) {
        is DetailBankCardFieldUiModel.Text -> {
            when (val editor = field.editor) {
                is DetailBankCardEditorUiState.Editing -> {
                    BankCardEditor(field.field, stringResource(field.labelRes), editor.draft, onAction)
                }

                DetailBankCardEditorUiState.ReadOnly -> {
                    DetailValueCard(
                        title = stringResource(field.labelRes),
                        value = field.value,
                        isRevealed = true,
                        onCopy = { onAction(DetailUiAction.CopyField(field.field)) },
                    )
                }

                DetailBankCardEditorUiState.Available -> {
                    DetailValueCard(
                        title = stringResource(field.labelRes),
                        value = field.value,
                        isRevealed = true,
                        onCopy = { onAction(DetailUiAction.CopyField(field.field)) },
                        onLongPress = { onAction(DetailUiAction.StartFieldEdit(field.field)) },
                    )
                }
            }
        }

        is DetailBankCardFieldUiModel.Protected -> {
            when (val editor = field.editor) {
                is DetailBankCardEditorUiState.Editing -> {
                    BankCardEditor(field.field, stringResource(field.labelRes), editor.draft, onAction)
                }

                DetailBankCardEditorUiState.Available,
                DetailBankCardEditorUiState.ReadOnly,
                -> {
                    DetailValueCard(
                        title = stringResource(field.labelRes),
                        value = field.displayText,
                        isRevealed = field.isRevealed,
                        maskStyle = if (field.compactMask) MaskStyle.SHORT else MaskStyle.DEFAULT,
                        onCopy = { onAction(DetailUiAction.CopyField(field.field)) },
                        onLongPress =
                            if (editor == DetailBankCardEditorUiState.Available) {
                                { onAction(DetailUiAction.StartFieldEdit(field.field)) }
                            } else {
                                null
                            },
                        onRevealClick = { onAction(DetailUiAction.ToggleFieldVisibility(field.field)) },
                    )
                }
            }
        }

        is DetailBankCardFieldUiModel.ReadOnly -> {
            DetailValueCard(
                title = stringResource(field.labelRes),
                value = field.value,
                isRevealed = true,
                onCopy = { onAction(DetailUiAction.CopyField(field.field)) },
            )
        }
    }
}

@Composable
private fun BankCardEditor(
    field: DetailField,
    label: String,
    draft: String,
    onAction: (DetailUiAction) -> Unit,
) {
    OutlinedTextField(
        value = draft,
        onValueChange = { onAction(DetailUiAction.UpdateFieldDraft(field, it)) },
        label = { Text(stringResource(R.string.field_edit_action, label)) },
        modifier = Modifier.fillMaxWidth().excludeFromAutofill(),
        trailingIcon = {
            IconButton(onClick = { onAction(DetailUiAction.SaveField(field, draft)) }) {
                Icon(Icons.Default.Check, stringResource(R.string.save), tint = MaterialTheme.colorScheme.primary)
            }
        },
        singleLine = true,
    )
}
