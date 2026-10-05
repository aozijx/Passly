package com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar

import androidx.compose.runtime.Immutable
@Immutable
data class VaultTopBarUiState(
    val query: String,
    val menu: VaultMenuUiState,
    val collapseOnScroll: Boolean,
    val collapseQuickFilterOnScroll: Boolean,
    val hideSystemBars: Boolean,
)
