package com.aozijx.passly.feature.vault.presentation.detail

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.aozijx.passly.presentation.shared.components.VaultIcons
import java.net.URI
import java.net.URISyntaxException

sealed interface DetailFaviconSource {
    data object InferredDefault : DetailFaviconSource

    data class BuiltIn(
        val key: String,
        val colorToken: String?,
    ) : DetailFaviconSource

    data class PrivateImage(
        val localPath: String,
    ) : DetailFaviconSource
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

sealed interface DetailFaviconCropInput {
    data object None : DetailFaviconCropInput

    data class Ready(val path: String) : DetailFaviconCropInput
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
        val cropInput: DetailFaviconCropInput = DetailFaviconCropInput.None,
        val showDiscardConfirmation: Boolean = false,
    ) : DetailFaviconEditorState {
        val dirty: Boolean get() = source != initialSource || cropInput != DetailFaviconCropInput.None
        val matchingIcons: List<FaviconIconUiModel>
            get() =
                VaultIcons.search(searchQuery).map { icon ->
                    FaviconIconUiModel(icon.key, icon.labelRes, icon.imageVector)
                }
        val canDownloadImageUrl: Boolean
            get() = imageUrl.canDownloadImageUrl()
    }
}

data class FaviconIconUiModel(
    val key: String,
    @param:StringRes val labelRes: Int,
    val imageVector: ImageVector,
)

private fun String.canDownloadImageUrl(): Boolean {
    val uri =
        try {
            URI(trim())
        } catch (_: URISyntaxException) {
            return false
        }
    return uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
}

sealed interface DetailFaviconWorkState {
    data object Idle : DetailFaviconWorkState

    data object Processing : DetailFaviconWorkState

    data class Failed(
        val error: DetailFaviconProcessingError,
    ) : DetailFaviconWorkState
}
