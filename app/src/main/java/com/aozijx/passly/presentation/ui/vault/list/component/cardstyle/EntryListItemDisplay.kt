package com.aozijx.passly.presentation.ui.vault.list.component.cardstyle

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import com.aozijx.passly.presentation.ui.shared.components.VaultItemIcon
import com.aozijx.passly.presentation.ui.shared.entry.EntryTypeUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.ui.shared.entry.labelRes

@Composable
internal fun VaultListItemUiModel.categoryOrTemplateLabel(): String =
    category ?: stringResource(entryType.labelRes)

@Composable
internal fun VaultListItemIcon(modifier: Modifier, item: VaultListItemUiModel) = VaultItemIcon(
    modifier = modifier,
    iconName = item.iconName,
    iconCustomPath = item.iconCustomPath,
    associatedAppPackage = item.associatedAppPackage,
    entryTypeKey = item.entryType.name,
    title = item.title,
    username = item.username,
    associatedDomain = item.associatedDomain,
)
