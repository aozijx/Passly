package com.aozijx.passly.feature.backup

import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.feature.backup.contract.BackupUiState
import javax.inject.Inject

class BackupSessionPolicy @Inject constructor(
    private val vaultAccessState: VaultAccessState
) {
    fun canPrepareRegularExport(): BackupSessionCheck =
        requireFullVaultAccess("当前会话不能导出普通备份")

    fun canPrepareRecoveryExport(): BackupSessionCheck =
        if (vaultAccessState.isRecoveryMode()) {
            BackupSessionCheck.Allowed
        } else {
            BackupSessionCheck.Denied("恢复导出只能在恢复模式中使用")
        }

    fun canPrepareImport(): BackupSessionCheck =
        requireFullVaultAccess("当前会话不能导入备份")

    fun canUsePendingOperation(state: BackupUiState): BackupSessionCheck =
        if (state.isRecoveryExport) {
            canPrepareRecoveryExport()
        } else {
            requireFullVaultAccess("当前会话不能执行备份操作")
        }

    private fun requireFullVaultAccess(message: String): BackupSessionCheck =
        if (vaultAccessState.hasFullVaultAccess()) {
            BackupSessionCheck.Allowed
        } else {
            BackupSessionCheck.Denied(message)
        }
}

sealed interface BackupSessionCheck {
    data object Allowed : BackupSessionCheck
    data class Denied(val message: String) : BackupSessionCheck
}
