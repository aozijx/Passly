package com.aozijx.passly.feature.vault.presentation.detail.model

import androidx.annotation.StringRes
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconUiModel

data class DetailIconCardUiModel(
    val icon: VaultItemIconUiModel,
    @param:StringRes val iconDescriptionRes: Int,
)
