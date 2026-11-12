package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.autofill.excludeFromAutofill
import com.aozijx.passly.core.ui.components.MaskStyle
import com.aozijx.passly.feature.vault.presentation.detail.DetailField
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.model.CredentialFieldUiState

@Composable
fun CredentialSection(
    fields: List<CredentialFieldUiState>,
    onAction: (DetailUiAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        fields.forEach { field ->
            CredentialField(field, onAction)
        }
    }
}

@Composable
private fun CredentialField(
    state: CredentialFieldUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    val title = stringResource(state.titleRes)
    when (state) {
        is CredentialFieldUiState.Hidden -> {
            DetailValueCard(
                title = title,
                value = "",
                isRevealed = false,
                onCopy = { onAction(DetailUiAction.CopyField(state.field)) },
                onActivate = { onAction(DetailUiAction.ToggleFieldVisibility(state.field)) },
            )
        }

        is CredentialFieldUiState.Revealed -> {
            DetailValueCard(
                title = title,
                value = state.value.useChars(::String),
                isRevealed = true,
                onCopy = { onAction(DetailUiAction.CopyField(state.field)) },
                onActivate = { onAction(DetailUiAction.ToggleFieldVisibility(state.field)) },
                onLongPress = { onAction(DetailUiAction.StartFieldEdit(state.field)) },
            )
        }

        is CredentialFieldUiState.Editing -> {
            InfoGroupCard(title) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.draft,
                        onValueChange = { onAction(DetailUiAction.UpdateFieldDraft(state.field, it)) },
                        modifier = Modifier.fillMaxWidth().excludeFromAutofill(),
                        label = { Text(stringResource(R.string.field_edit_action, title)) },
                        singleLine = true,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.showModifiedHint) {
                            Text(
                                stringResource(R.string.vault_edit_modified_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { onAction(DetailUiAction.SaveField(state.field, state.draft)) }) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }
        }
    }
}
