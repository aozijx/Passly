package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

@Composable
fun DetailValueCard(
    title: String,
    value: String,
    isRevealed: Boolean,
    onCopy: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    maskStyle: MaskStyle = MaskStyle.DEFAULT,
    onActivate: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onRevealClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val interactionModifier =
        if (onActivate == null && onLongPress == null) {
            Modifier
        } else {
            Modifier.combinedClickable(
                onClick = onActivate ?: {},
                onLongClick =
                    onLongPress?.let {
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            it()
                        }
                    },
            )
        }

    InfoGroupCard(title) {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .then(interactionModifier)
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MaskedText(
                text = value,
                isRevealed = isRevealed,
                maskStyle = maskStyle,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Medium,
            )
            onCopy?.let { copy ->
                IconButton(onClick = copy, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            onRevealClick?.let { reveal ->
                IconButton(onClick = reveal, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription =
                            stringResource(
                                if (isRevealed) R.string.hide_password else R.string.show_password,
                            ),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}
