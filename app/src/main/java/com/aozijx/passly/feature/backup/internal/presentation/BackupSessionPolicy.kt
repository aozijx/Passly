package com.aozijx.passly.feature.backup.internal.presentation

import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.feature.backup.internal.contract.BackupUiState
import javax.inject.Inject

class BackupSessionPolicy @Inject constructor(
    private val vaultAccessState: VaultAccessState
) {
    fun regularExportDenial(): String? =
        requireFullVaultAccess("当前会话不能导出普通备份")

    fun recoveryExportDenial(): String? =
        if (vaultAccessState.isRecoveryMode()) {
            null
        } else {
            "恢复导出只能在恢复模式中使用"
        }

    fun importDenial(): String? =
        requireFullVaultAccess("当前会话不能导入备份")

    fun pendingOperationDenial(state: BackupUiState): String? =
        if (state.isRecoveryExport) {
            recoveryExportDenial()
        } else {
            requireFullVaultAccess("当前会话不能执行备份操作")
        }

    private fun requireFullVaultAccess(message: String): String? =
        if (vaultAccessState.hasFullVaultAccess()) {
            null
        } else {
            message
        }
}
