package com.aozijx.passly.feature.backup.internal.presentation

import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.feature.backup.internal.contract.BackupUiState
import javax.inject.Inject

internal class BackupSessionPolicy @Inject constructor(
    private val vaultAccessState: VaultAccessState,
) {
    fun regularExportDenial(): BackupSessionDenial? = requireFullVaultAccess()

    fun recoveryExportDenial(): BackupSessionDenial? =
        if (vaultAccessState.isRecoveryMode()) {
            null
        } else {
            BackupSessionDenial.RECOVERY_MODE_REQUIRED
        }

    fun importDenial(): BackupSessionDenial? = requireFullVaultAccess()

    fun pendingOperationDenial(state: BackupUiState): BackupSessionDenial? =
        if (state.isRecoveryExport) {
            recoveryExportDenial()
        } else {
            requireFullVaultAccess()
        }

    private fun requireFullVaultAccess(): BackupSessionDenial? =
        if (vaultAccessState.hasFullVaultAccess()) {
            null
        } else {
            BackupSessionDenial.FULL_VAULT_ACCESS_REQUIRED
        }
}

internal enum class BackupSessionDenial {
    FULL_VAULT_ACCESS_REQUIRED,
    RECOVERY_MODE_REQUIRED,
}
