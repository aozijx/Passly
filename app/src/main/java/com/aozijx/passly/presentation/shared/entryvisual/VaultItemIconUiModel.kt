package com.aozijx.passly.presentation.shared.entryvisual

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.aozijx.passly.presentation.shared.components.VaultIcons
import com.aozijx.passly.presentation.shared.media.LocalIconImageModel
import com.aozijx.passly.presentation.shared.media.toLocalIconImageModel

@Immutable
data class VaultItemIconUiModel(
    val image: VaultItemIconImageUiState,
    val glyph: VaultItemIconGlyphUiState,
    val associatedApp: VaultItemAssociatedAppUiState,
    val visualCategory: EntryVisualCategory,
)

@Immutable
sealed interface VaultItemIconImageUiState {
    data object Default : VaultItemIconImageUiState

    data class Custom(
        val model: LocalIconImageModel,
    ) : VaultItemIconImageUiState
}

@Immutable
sealed interface VaultItemIconGlyphUiState {
    data class Named(
        val name: String,
        val imageVector: ImageVector,
        val tint: VaultItemIconTintUiState,
    ) : VaultItemIconGlyphUiState

    data object CategoryFallback : VaultItemIconGlyphUiState
}

@Immutable
sealed interface VaultItemIconTintUiState {
    data object ThemeDefault : VaultItemIconTintUiState

    data class StorageToken(
        val value: String,
    ) : VaultItemIconTintUiState
}

@Immutable
sealed interface VaultItemAssociatedAppUiState {
    data object None : VaultItemAssociatedAppUiState

    data class Package(
        val name: String,
    ) : VaultItemAssociatedAppUiState {
        init {
            require(name.isNotBlank())
        }
    }
}

internal fun vaultItemIconUiModel(
    iconName: String?,
    customImagePath: String?,
    iconColor: String? = null,
    associatedAppPackage: String?,
    visualCategory: EntryVisualCategory,
): VaultItemIconUiModel {
    val image =
        toLocalIconImageModel(customImagePath)
            ?.let(VaultItemIconImageUiState::Custom)
            ?: VaultItemIconImageUiState.Default
    val glyph =
        iconName
            ?.takeIf(String::isNotBlank)
            ?.let { name ->
                VaultIcons.findDefinition(name)?.imageVector?.let { imageVector ->
                    VaultItemIconGlyphUiState.Named(
                        name = name,
                        imageVector = imageVector,
                        tint =
                            iconColor
                                ?.takeIf(String::isNotBlank)
                                ?.let(VaultItemIconTintUiState::StorageToken)
                                ?: VaultItemIconTintUiState.ThemeDefault,
                    )
                }
            }
            ?: VaultItemIconGlyphUiState.CategoryFallback
    val associatedApp =
        associatedAppPackage
            ?.takeIf(String::isNotBlank)
            ?.let(VaultItemAssociatedAppUiState::Package)
            ?: VaultItemAssociatedAppUiState.None
    return VaultItemIconUiModel(image, glyph, associatedApp, visualCategory)
}
