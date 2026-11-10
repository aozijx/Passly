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

@Composable
fun SensitiveFieldCard(
    title: String,
    isEditing: Boolean,
    editedValue: String,
    revealedValue: String?,
    onEditToggle: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
    onReveal: () -> Unit,
    onCopy: () -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
    maskStyle: MaskStyle = MaskStyle.DEFAULT,
) {
    if (isEditing) {
        InfoGroupCard(title) {
            Column(
                modifier = modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = editedValue,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth().excludeFromAutofill(),
                    label = { Text(stringResource(R.string.field_edit_action, title)) },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (revealedValue != null && editedValue != revealedValue) {
                        Text(
                            stringResource(R.string.vault_edit_modified_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { onSave(editedValue) }) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    } else {
        DetailValueCard(
            title = title,
            value = revealedValue.orEmpty(),
            isRevealed = revealedValue != null,
            onCopy = onCopy,
            modifier = modifier,
            maskStyle = maskStyle,
            onActivate = onReveal,
            onLongPress =
                revealedValue?.let { value ->
                    { onValueChange(value); onEditToggle(true) }
                },
        )
    }
}
