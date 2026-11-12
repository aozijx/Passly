package com.aozijx.passly.feature.vault.presentation.detail

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.aozijx.passly.presentation.shared.components.VaultIcons

sealed interface DetailFaviconSource {
    data object InferredDefault : DetailFaviconSource

    sealed interface ColorSelection {
        data object Automatic : ColorSelection

        data class Token(
            val value: String,
        ) : ColorSelection
    }

    data class BuiltIn(
        val key: String,
        val color: ColorSelection = ColorSelection.Automatic,
    ) : DetailFaviconSource

    data class PrivateImage(
        val localPath: String,
    ) : DetailFaviconSource
}

enum class DetailFaviconTab {
    ICON_LIBRARY,
    CUSTOM_IMAGE,
}

sealed interface DetailFaviconCropInput {
    data object None : DetailFaviconCropInput

    data class Ready(
        val path: String,
    ) : DetailFaviconCropInput {
        val isGif: Boolean get() = path.endsWith(".gif", ignoreCase = true)
    }
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
        val canSubmitImageUrl: Boolean
            get() = imageUrl.isNotBlank()
    }
}

data class FaviconIconUiModel(
    val key: String,
    @param:StringRes val labelRes: Int,
    val imageVector: ImageVector,
)

sealed interface DetailFaviconWorkState {
    data object Idle : DetailFaviconWorkState

    data object Processing : DetailFaviconWorkState

    data class Failed(
        @get:StringRes val messageRes: Int,
    ) : DetailFaviconWorkState
}
