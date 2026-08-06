package com.aozijx.passly.feature.vault

import com.aozijx.passly.domain.authentication.VaultAccessState
import javax.inject.Inject

class VaultAccessPolicy @Inject constructor(
    private val vaultAccessState: VaultAccessState
) {
    fun hasFullAccess(): Boolean =
        vaultAccessState.hasFullVaultAccess()
}
