package com.aozijx.passly.presentation.ui.settings.appearance.model

data class LibraryQuickFilterOptionUiModel(
    val filter: LibraryQuickFilterUiModel,
    val selected: Boolean,
)

enum class LibraryQuickFilterUiModel {
    PASSWORDS,
    TOTP,
}
