package com.aozijx.passly.feature.detail.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.MaskStyle
import com.aozijx.passly.core.ui.components.MaskedText
import com.aozijx.passly.core.ui.components.PasslyOutlinedTextField

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
    maskStyle: MaskStyle = MaskStyle.DEFAULT
) {
    val haptic = LocalHapticFeedback.current
    InfoGroupCard(title = title) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .then(
                    if (isEditing) Modifier
                    else Modifier.combinedClickable(
                        onLongClick = {
                            if (revealedValue != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onValueChange(revealedValue)
                                onEditToggle(true)
                            }
                        },
                        onClick = onReveal
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PasslyOutlinedTextField(
                        value = editedValue,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.field_edit_action, title),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (revealedValue != null && (editedValue != revealedValue)) {
                            Text(
                                stringResource(R.string.vault_edit_modified_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
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
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MaskedText(
                        text = revealedValue,
                        isRevealed = revealedValue != null,
                        maskStyle = maskStyle,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.Medium
                    )

                    IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.copy),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
