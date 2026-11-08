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
    val category: String?,
    val favorite: Boolean,
    val associatedDomain: String?,
    val associatedAppPackage: String?,
    val iconName: String?,
    val iconCustomPath: String?,
    val iconImageModel: LocalIconImageModel?,
    val visualCategory: EntryVisualCategory,
    val hasPassword: Boolean,
    val hasOtp: Boolean,
    val otpKind: VaultOtpKindUiModel?,
    val otpPreview: String?,
    val passwordCardSecondaryText: VaultPasswordCardSecondaryTextUiModel,
    val passwordCardSupportingText: VaultPasswordCardSupportingTextUiModel,
)

sealed interface VaultPasswordCardSecondaryTextUiModel {
    data class Value(val text: String) : VaultPasswordCardSecondaryTextUiModel

    data class EntryTypeLabel(val entryType: EntryType) : VaultPasswordCardSecondaryTextUiModel
}

enum class VaultPasswordCardSupportingTextUiModel { FAVORITE, AUTOFILL, PROTECTED }

enum class VaultOtpKindUiModel { STANDARD, STEAM }
