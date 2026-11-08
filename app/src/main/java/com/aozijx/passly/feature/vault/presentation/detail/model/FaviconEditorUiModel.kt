package com.aozijx.passly.feature.vault.presentation.detail.model

import androidx.annotation.StringRes
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.presentation.shared.components.EntryVisualCategory
import com.aozijx.passly.presentation.shared.media.LocalIconImageModel

data class DetailIconCardUiModel(
    val iconName: String?,
    val iconCustomPath: String?,
    val iconImageModel: LocalIconImageModel?,
    val iconColor: String?,
    val associatedAppPackage: String?,
    val entryType: EntryType,
    val title: String,
    val username: String,
    val associatedDomain: String?,
    val visualCategory: EntryVisualCategory,
    @param:StringRes val iconDescriptionRes: Int,
)
