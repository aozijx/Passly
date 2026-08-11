package com.aozijx.passly.feature.vault

import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import javax.inject.Inject

class SecureSessionAccessPolicy @Inject constructor(
    private val vaultAccessState: SecureSessionAccessState
) {
    fun hasFullAccess(): Boolean =
        vaultAccessState.hasFullSecureSessionAccess()
}
