package com.aozijx.passly.presentation.feature.vault.detail

sealed interface DetailFaviconSource {
    data object InferredDefault : DetailFaviconSource
    data class BuiltIn(
        val key: String,
        val colorToken: String?,
    ) : DetailFaviconSource
    data class PrivateImage(val localPath: String) : DetailFaviconSource
}

enum class DetailFaviconTab {
    ICON_LIBRARY,
    CUSTOM_IMAGE,
}

enum class DetailFaviconProcessingError {
    INVALID_URL,
    URL_NOT_ALLOWED,
    DOWNLOAD_FAILED,
    NOT_IMAGE,
    IMAGE_TOO_LARGE,
    INVALID_IMAGE,
    SAVE_FAILED,
}

sealed interface DetailFaviconEditorState {
    data object Closed : DetailFaviconEditorState

    data class Open(
        val initialSource: DetailFaviconSource,
        val source: DetailFaviconSource = initialSource,
        val selectedTab: DetailFaviconTab = DetailFaviconTab.ICON_LIBRARY,
        val searchQuery: String = "",
        val imageUrl: String = "",
        val work: DetailFaviconWorkState = DetailFaviconWorkState.Idle,
        val pendingInputPath: String? = null,
        val promotedCandidatePath: String? = null,
        val overlay: DetailFaviconOverlay = DetailFaviconOverlay.None,
    ) : DetailFaviconEditorState {
        val dirty: Boolean get() = source != initialSource || pendingInputPath != null
    }
}

sealed interface DetailFaviconWorkState {
    data object Idle : DetailFaviconWorkState
    data object Processing : DetailFaviconWorkState
    data class Failed(val error: DetailFaviconProcessingError) : DetailFaviconWorkState
}

sealed interface DetailFaviconOverlay {
    data object None : DetailFaviconOverlay
    data object ConfirmDiscard : DetailFaviconOverlay
}
