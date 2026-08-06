package com.aozijx.passly.feature.detail

import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.feature.detail.contract.DetailIntent
import javax.inject.Inject

class DetailAccessPolicy @Inject constructor(
    private val vaultAccessState: VaultAccessState
) {
    fun canHandle(intent: DetailIntent): Boolean =
        intent is DetailIntent.ClearSensitiveState || hasFullAccess()

    fun hasFullAccess(): Boolean =
        vaultAccessState.hasFullVaultAccess()
}
