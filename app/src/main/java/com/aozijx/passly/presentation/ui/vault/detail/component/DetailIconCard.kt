package com.aozijx.passly.presentation.ui.vault.detail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.shared.components.VaultIcons
import com.aozijx.passly.presentation.ui.shared.components.VaultItemIcon
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailIconCardUiModel

@Composable
fun DetailIconCard(
    model: DetailIconCardUiModel,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconDescription = when {
        !model.iconCustomPath.isNullOrBlank() ->
            stringResource(R.string.vault_detail_favicon_custom_image)
        !model.iconName.isNullOrBlank() -> VaultIcons.findDefinition(model.iconName)
            ?.let { stringResource(it.labelRes) }
            ?: stringResource(R.string.vault_detail_favicon_built_in)
        else -> stringResource(R.string.vault_detail_favicon_default)
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VaultItemIcon(
                modifier = Modifier,
                iconName = model.iconName,
                iconCustomPath = model.iconCustomPath,
                associatedAppPackage = model.associatedAppPackage,
                entryTypeKey = model.entryTypeKey,
                title = model.title,
                username = model.username,
                associatedDomain = model.associatedDomain,
                iconColor = model.iconColor,
            )
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.vault_detail_favicon_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = iconDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalIconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.vault_detail_favicon_edit),
                )
            }
        }
    }
}
