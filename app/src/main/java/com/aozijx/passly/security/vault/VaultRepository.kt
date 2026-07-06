package com.aozijx.passly.security.vault

import com.aozijx.passly.security.crypto.DekManager
import com.aozijx.passly.security.envelope.EnvelopeStore

/**
 * Vault 仓库 —— 协调 DekManager、EnvelopeStore、VerificationTag。
 *
 * 对外提供统一的 Vault 管理 API：
 * - 保险箱状态查询
 * - 信封审计
 * - 保险箱删除
 */
class VaultRepository(
    private val dekManager: DekManager,
    private val envelopeStore: EnvelopeStore
) {
    /**
     * 获取当前 Vault 信息。
     */
    fun getVaultInfo(): VaultInfo {
        val envelopeIds = envelopeStore.getAllIds()
        return VaultInfo(
            isInitialized = envelopeStore.hasAny(),
            envelopeCount = envelopeIds.size,
            availableMethods = envelopeIds.toList().sorted()
        )
    }

    /**
     * 删除 Vault（不可逆）。
     */
    fun deleteVault() {
        dekManager.deleteVault()
    }
}
