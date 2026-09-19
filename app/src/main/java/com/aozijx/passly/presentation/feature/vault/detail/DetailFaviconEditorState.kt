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

data class DetailFaviconEditorState(
    val visible: Boolean = false,
    val initialSource: DetailFaviconSource = DetailFaviconSource.InferredDefault,
    val source: DetailFaviconSource = DetailFaviconSource.InferredDefault,
    val selectedTab: DetailFaviconTab = DetailFaviconTab.ICON_LIBRARY,
    val searchQuery: String = "",
    val imageUrl: String = "",
    val processing: Boolean = false,
    val pendingInputPath: String? = null,
    val promotedCandidatePath: String? = null,
    val processingError: DetailFaviconProcessingError? = null,
    val confirmDiscard: Boolean = false,
) {
    val dirty: Boolean get() = source != initialSource || pendingInputPath != null
}
