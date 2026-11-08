package com.aozijx.passly.feature.vault.presentation.detail.ui.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.presentation.detail.DetailFaviconSource
import com.aozijx.passly.feature.vault.presentation.detail.DetailFaviconTab
import com.aozijx.passly.presentation.shared.components.EntryVisualCategory
import com.aozijx.passly.presentation.shared.media.LocalIconImageModel

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
        val initialSource: DetailFaviconSource,
        val source: DetailFaviconSource,
        val selectedTab: DetailFaviconTab,
        val searchQuery: String,
        val matchingIcons: List<FaviconIconUiModel>,
        val imageUrl: String,
        val imageUrlDownloadState: FaviconImageUrlDownloadUiState,
        val work: FaviconWorkUiState,
        val pendingInputPath: String?,
        val promotedCandidatePath: String?,
        val overlay: FaviconOverlayUiState,
    ) : DetailFaviconEditorUiModel {
        val dirty: Boolean get() = source != initialSource || pendingInputPath != null
    }
}

data class FaviconIconUiModel(
    val key: String,
    @param:StringRes val labelRes: Int,
    val imageVector: ImageVector,
)

sealed interface FaviconImageUrlDownloadUiState {
    data object Disabled : FaviconImageUrlDownloadUiState

    data object Enabled : FaviconImageUrlDownloadUiState
}

sealed interface FaviconWorkUiState {
    data object Idle : FaviconWorkUiState

    data object Processing : FaviconWorkUiState

    data class Failed(
        val error: FaviconProcessingErrorUiModel,
    ) : FaviconWorkUiState
}

sealed interface FaviconOverlayUiState {
    data object None : FaviconOverlayUiState

    data object ConfirmDiscard : FaviconOverlayUiState
}

data class DetailIconCardUiModel(
    val iconName: String?,
    val iconCustomPath: String?,
    val iconImageModel: LocalIconImageModel?,
    val iconColor: String?,
    val associatedAppPackage: String?,
    val entryType: EntryType,
    val title: String,
    val username: String,
    val associatedDomain: String?,
    val visualCategory: EntryVisualCategory,
    @param:StringRes val iconDescriptionRes: Int,
)
