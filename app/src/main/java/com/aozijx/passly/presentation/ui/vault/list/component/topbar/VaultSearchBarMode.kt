package com.aozijx.passly.presentation.ui.vault.list.component.topbar

internal enum class VaultSearchBarMode { DEFAULT, EDITING, RESULT }

internal fun resolveVaultSearchBarMode(
    isSearchActive: Boolean,
    isEditing: Boolean,
    query: String,
): VaultSearchBarMode = when {
    isEditing -> VaultSearchBarMode.EDITING
    isSearchActive && query.isNotBlank() -> VaultSearchBarMode.RESULT
    else -> VaultSearchBarMode.DEFAULT
}

internal fun resolveVaultSearchBarExpansion(
    pullProgress: Float,
    mode: VaultSearchBarMode,
): Float = if (mode == VaultSearchBarMode.EDITING) {
    1f
} else {
    pullProgress.coerceIn(0f, 1f)
}
