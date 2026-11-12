package com.aozijx.passly.feature.vault.presentation.list.ui.component.cardstyle

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemCategoryLabel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.shared.components.VaultItemIcon

@Composable
internal fun VaultListItemCategoryLabel.displayText(): String =
    when (this) {
        is VaultListItemCategoryLabel.Tag -> value
        is VaultListItemCategoryLabel.EntryTypeName -> stringResource(labelResource)
    }

@Composable
internal fun VaultListItemIcon(
    modifier: Modifier,
    item: VaultListItemUiModel,
) = VaultItemIcon(
    modifier = modifier,
    model = item.icon,
)
