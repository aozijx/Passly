package com.aozijx.passly.feature.vault.presentation.list.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.otp.presentation.OtpPresentation
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconUiModel

@Immutable
data class VaultListItemUiModel(
    val id: String,
    val entryType: EntryType,
    val title: String,
    val username: String,
    val categoryLabel: VaultListItemCategoryLabel,
    val favorite: Boolean,
    val icon: VaultItemIconUiModel,
    val hasPassword: Boolean,
    val otpPresentation: OtpPresentation,
    val passwordCardSecondaryText: VaultPasswordCardSecondaryTextUiModel,
    val passwordCardSupportingText: VaultPasswordCardSupportingTextUiModel,
)

@Immutable
sealed interface VaultListItemCategoryLabel {
    data class Tag(
        val value: String,
    ) : VaultListItemCategoryLabel

    data class EntryTypeName(
        @field:StringRes val labelResource: Int,
    ) : VaultListItemCategoryLabel
}

sealed interface VaultPasswordCardSecondaryTextUiModel {
    data class Value(
        val text: String,
    ) : VaultPasswordCardSecondaryTextUiModel

    data class EntryTypeLabel(
        @field:StringRes val labelResource: Int,
    ) : VaultPasswordCardSecondaryTextUiModel
}

enum class VaultPasswordCardSupportingTextUiModel { FAVORITE, AUTOFILL, PROTECTED }
