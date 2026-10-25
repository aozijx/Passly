package com.aozijx.passly.feature.vault.presentation.detail.ui.model

import com.aozijx.passly.presentation.shared.entry.EntryTypeUiModel

sealed interface FaviconDraftSourceUiModel {
    data object InferredDefault : FaviconDraftSourceUiModel
    data class BuiltIn(
        val key: String,
        val colorToken: String?,
    ) : FaviconDraftSourceUiModel
    data class PrivateImage(val localPath: String) : FaviconDraftSourceUiModel
}

enum class FaviconEditorTabUiModel {
    ICON_LIBRARY,
    CUSTOM_IMAGE,
}

enum class FaviconProcessingErrorUiModel {
    INVALID_URL,
    URL_NOT_ALLOWED,
    DOWNLOAD_FAILED,
    NOT_IMAGE,
    IMAGE_TOO_LARGE,
    INVALID_IMAGE,
    SAVE_FAILED,
}

sealed interface DetailFaviconEditorUiModel {
    data object Hidden : DetailFaviconEditorUiModel

    data class Visible(
        val initialSource: FaviconDraftSourceUiModel,
        val source: FaviconDraftSourceUiModel,
        val selectedTab: FaviconEditorTabUiModel,
        val searchQuery: String,
        val imageUrl: String,
        val work: FaviconWorkUiState,
        val pendingInputPath: String?,
        val promotedCandidatePath: String?,
        val overlay: FaviconOverlayUiState,
    ) : DetailFaviconEditorUiModel {
        val dirty: Boolean get() = source != initialSource || pendingInputPath != null
    }
}

sealed interface FaviconWorkUiState {
    data object Idle : FaviconWorkUiState
    data object Processing : FaviconWorkUiState
    data class Failed(val error: FaviconProcessingErrorUiModel) : FaviconWorkUiState
}

sealed interface FaviconOverlayUiState {
    data object None : FaviconOverlayUiState
    data object ConfirmDiscard : FaviconOverlayUiState
}

data class DetailIconCardUiModel(
    val iconName: String?,
    val iconCustomPath: String?,
    val iconColor: String?,
    val associatedAppPackage: String?,
    val entryType: EntryTypeUiModel,
    val title: String,
    val username: String,
    val associatedDomain: String?,
)
