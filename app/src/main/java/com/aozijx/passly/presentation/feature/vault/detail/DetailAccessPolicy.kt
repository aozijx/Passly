package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import javax.inject.Inject

class DetailAccessPolicy @Inject constructor(
    private val vaultAccessState: SecureSessionAccessState
) {
    fun canHandle(action: DetailUiAction): Boolean =
        action is DetailUiAction.ClearSensitiveState || action.isTagAction() || hasFullAccess()

    fun hasFullAccess(): Boolean =
        vaultAccessState.hasFullSecureSessionAccess()
}

private fun DetailUiAction.isTagAction(): Boolean = when (this) {
    DetailUiAction.OpenTagEditor,
    is DetailUiAction.UpdateTagInput,
    is DetailUiAction.SubmitTag,
    is DetailUiAction.RemoveTag,
    DetailUiAction.SaveTags,
    DetailUiAction.DismissTagEditor,
    DetailUiAction.ConfirmDiscardTags,
    DetailUiAction.KeepEditingTags,
    -> true

    else -> false
}
