package com.aozijx.passly.feature.backup.internal.presentation

import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import javax.inject.Inject

internal class BackupSessionPolicy @Inject constructor(
    private val vaultAccessState: SecureSessionAccessState,
) {
    fun regularExportDenial(): BackupSessionDenial? = requireFullSecureSessionAccess()

    fun importDenial(): BackupSessionDenial? = requireFullSecureSessionAccess()

    fun pendingOperationDenial(): BackupSessionDenial? =
        requireFullSecureSessionAccess()

    private fun requireFullSecureSessionAccess(): BackupSessionDenial? =
        if (vaultAccessState.hasFullSecureSessionAccess()) {
            null
        } else {
            BackupSessionDenial.FULL_VAULT_ACCESS_REQUIRED
        }
}

internal enum class BackupSessionDenial {
    FULL_VAULT_ACCESS_REQUIRED,
}
