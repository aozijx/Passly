package com.aozijx.passly.feature.backup.internal.presentation

import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.feature.backup.internal.contract.BackupUiState
import javax.inject.Inject

internal class BackupSessionPolicy @Inject constructor(
    private val vaultAccessState: SecureSessionAccessState,
) {
    fun regularExportDenial(): BackupSessionDenial? = requireFullSecureSessionAccess()

    fun recoveryExportDenial(): BackupSessionDenial? =
        if (vaultAccessState.isRecoveryMode()) {
            null
        } else {
            BackupSessionDenial.RECOVERY_MODE_REQUIRED
        }

    fun importDenial(): BackupSessionDenial? = requireFullSecureSessionAccess()

    fun pendingOperationDenial(state: BackupUiState): BackupSessionDenial? =
        if (state.isRecoveryExport) {
            recoveryExportDenial()
        } else {
            requireFullSecureSessionAccess()
        }

    private fun requireFullSecureSessionAccess(): BackupSessionDenial? =
        if (vaultAccessState.hasFullSecureSessionAccess()) {
            null
        } else {
            BackupSessionDenial.FULL_VAULT_ACCESS_REQUIRED
        }
}

internal enum class BackupSessionDenial {
    FULL_VAULT_ACCESS_REQUIRED,
    RECOVERY_MODE_REQUIRED,
}
