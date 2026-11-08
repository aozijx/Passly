package com.aozijx.passly.feature.vault.presentation.list.ui.component.cardstyle

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.shared.components.VaultItemIcon
import com.aozijx.passly.presentation.shared.entry.labelRes

@Composable
internal fun VaultListItemUiModel.categoryOrTemplateLabel(): String = category ?: stringResource(entryType.labelRes)

@Composable
internal fun VaultListItemIcon(
    modifier: Modifier,
    item: VaultListItemUiModel,
) = VaultItemIcon(
    modifier = modifier,
    iconName = item.iconName,
    customImageModel = item.iconImageModel,
    associatedAppPackage = item.associatedAppPackage,
    visualCategory = item.visualCategory,
)
