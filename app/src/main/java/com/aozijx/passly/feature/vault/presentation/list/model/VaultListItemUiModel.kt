package com.aozijx.passly.feature.vault.presentation.list.model

import androidx.compose.runtime.Immutable
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.presentation.shared.components.EntryVisualCategory
import com.aozijx.passly.presentation.shared.media.LocalIconImageModel

@Immutable
data class VaultListItemUiModel(
    val id: String,
    val entryType: EntryType,
    val title: String,
    val username: String,
    val categoryLabel: VaultListItemCategoryLabel,
    val favorite: Boolean,
    val associatedDomain: String?,
    val associatedAppPackage: String?,
    val iconName: String?,
    val iconCustomPath: String?,
    val iconImageModel: LocalIconImageModel?,
    val visualCategory: EntryVisualCategory,
    val hasPassword: Boolean,
    val otpKind: VaultOtpKindUiModel,
    val otpPreview: String?,
    val passwordCardSecondaryText: VaultPasswordCardSecondaryTextUiModel,
    val passwordCardSupportingText: VaultPasswordCardSupportingTextUiModel,
)

@Immutable
sealed interface VaultListItemCategoryLabel {
    data class Tag(val value: String) : VaultListItemCategoryLabel

    data class EntryTypeName(val entryType: EntryType) : VaultListItemCategoryLabel
}

sealed interface VaultPasswordCardSecondaryTextUiModel {
    data class Value(val text: String) : VaultPasswordCardSecondaryTextUiModel

    data class EntryTypeLabel(val entryType: EntryType) : VaultPasswordCardSecondaryTextUiModel
}

enum class VaultPasswordCardSupportingTextUiModel { FAVORITE, AUTOFILL, PROTECTED }

enum class VaultOtpKindUiModel(
    val codeTypeLabel: String,
) {
    NOT_APPLICABLE(""),
    STANDARD("TOTP"),
    STEAM("STEAM"),
    ;

    val isApplicable: Boolean
        get() = this != NOT_APPLICABLE
}
