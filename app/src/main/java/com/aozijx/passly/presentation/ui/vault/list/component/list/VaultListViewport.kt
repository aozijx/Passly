package com.aozijx.passly.presentation.ui.vault.list.component.list

internal enum class VaultListViewport { MAIN, SEARCH }

internal fun resolveVaultListViewport(isSearchActive: Boolean): VaultListViewport =
    if (isSearchActive) VaultListViewport.SEARCH else VaultListViewport.MAIN
