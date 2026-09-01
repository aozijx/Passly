package com.aozijx.passly.presentation.ui.vault.detail.model

sealed interface FaviconDraftSourceUiModel {
    data object InferredDefault : FaviconDraftSourceUiModel
    data class BuiltIn(
        val key: String,
        val colorToken: String?,
    ) : FaviconDraftSourceUiModel
    data class PrivateImage(val stagedPath: String) : FaviconDraftSourceUiModel
}

enum class FaviconEditorTabUiModel {
    ICON_LIBRARY,
    UPLOAD,
    IMAGE_URL,
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

data class DetailFaviconEditorUiModel(
    val visible: Boolean = false,
    val initialSource: FaviconDraftSourceUiModel = FaviconDraftSourceUiModel.InferredDefault,
    val source: FaviconDraftSourceUiModel = FaviconDraftSourceUiModel.InferredDefault,
    val selectedTab: FaviconEditorTabUiModel = FaviconEditorTabUiModel.ICON_LIBRARY,
    val searchQuery: String = "",
    val imageUrl: String = "",
    val processing: Boolean = false,
    val pendingInputPath: String? = null,
    val processingError: FaviconProcessingErrorUiModel? = null,
    val confirmDiscard: Boolean = false,
    val presentationId: Long = 0,
) {
    val dirty: Boolean get() = source != initialSource || pendingInputPath != null
}

data class DetailIconCardUiModel(
    val iconName: String?,
    val iconCustomPath: String?,
    val iconColor: String?,
    val associatedAppPackage: String?,
    val entryTypeKey: String,
    val title: String,
    val username: String,
    val associatedDomain: String?,
)
