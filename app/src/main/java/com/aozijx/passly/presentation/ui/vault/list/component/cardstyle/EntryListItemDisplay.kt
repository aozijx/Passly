package com.aozijx.passly.presentation.ui.vault.list.component.cardstyle

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import com.aozijx.passly.core.ui.components.VaultItemIcon
import com.aozijx.passly.presentation.ui.vault.list.model.VaultEntryTypeUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.labelRes

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
