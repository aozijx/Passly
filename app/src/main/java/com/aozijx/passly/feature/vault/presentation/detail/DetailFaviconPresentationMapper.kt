package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.presentation.shared.components.VaultIcons
import java.net.URI
import java.net.URISyntaxException

internal fun DetailFaviconEditorState.toFaviconEditorUiModel(): DetailFaviconEditorUiModel =
    when (this) {
        DetailFaviconEditorState.Closed -> {
            DetailFaviconEditorUiModel.Hidden
        }

        is DetailFaviconEditorState.Open -> {
            DetailFaviconEditorUiModel.Visible(
                source = source,
                selectedTab = selectedTab,
                searchQuery = searchQuery,
                matchingIcons = VaultIcons.search(searchQuery).map { FaviconIconUiModel(it.key, it.labelRes, it.imageVector) },
                imageUrl = imageUrl,
                canDownloadImageUrl = imageUrl.canDownloadImageUrl(),
                dirty = dirty,
                work = work,
                pendingInputPath = pendingInputPath,
                promotedCandidatePath = promotedCandidatePath,
                showDiscardConfirmation = showDiscardConfirmation,
            )
        }
    }

private fun String.canDownloadImageUrl(): Boolean {
    val uri =
        try {
            URI(trim())
        } catch (_: URISyntaxException) {
            return false
        }
    return uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
}
