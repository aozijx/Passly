package com.aozijx.passly.feature.vault.presentation.detail

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

sealed interface DetailFaviconEditorUiModel {
    data object Hidden : DetailFaviconEditorUiModel

    data class Visible(
        val source: DetailFaviconSource,
        val selectedTab: DetailFaviconTab,
        val searchQuery: String,
        val matchingIcons: List<FaviconIconUiModel>,
        val imageUrl: String,
        val canDownloadImageUrl: Boolean,
        val dirty: Boolean,
        val work: DetailFaviconWorkState,
        val pendingInputPath: String?,
        val promotedCandidatePath: String?,
        val overlay: DetailFaviconOverlay,
    ) : DetailFaviconEditorUiModel
}

data class FaviconIconUiModel(
    val key: String,
    @param:StringRes val labelRes: Int,
    val imageVector: ImageVector,
)
