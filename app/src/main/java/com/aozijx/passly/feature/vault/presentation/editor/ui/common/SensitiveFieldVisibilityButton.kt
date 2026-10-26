package com.aozijx.passly.feature.vault.presentation.editor.ui.common

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.aozijx.passly.R

/** Accepts a field label resource, never the sensitive field value. Visibility remains caller-owned. */
@Composable
internal fun SensitiveFieldVisibilityButton(
    @StringRes fieldLabelRes: Int,
    visible: Boolean,
    onVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(fieldLabelRes)
    val state = stringResource(
        if (visible) R.string.vault_editor_field_visible else R.string.vault_editor_field_hidden,
    )
    IconToggleButton(
        checked = visible,
        onCheckedChange = onVisibilityChanged,
        modifier = modifier.semantics { stateDescription = state },
    ) {
        Icon(
            imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = stringResource(
                if (visible) R.string.vault_editor_hide_field else R.string.vault_editor_show_field,
                label,
            ),
        )
    }
}
