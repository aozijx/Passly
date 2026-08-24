package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import javax.inject.Inject

class DetailAccessPolicy @Inject constructor(
    private val vaultAccessState: SecureSessionAccessState
) {
    fun canHandle(action: DetailUiAction): Boolean =
        action is DetailUiAction.ClearSensitiveState || hasFullAccess()

    fun hasFullAccess(): Boolean =
        vaultAccessState.hasFullSecureSessionAccess()
}
