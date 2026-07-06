package com.aozijx.passly.security.vault

/**
 * Vault 元信息。
 *
 * 描述当前密码保险箱的摘要信息，不包含任何密钥材料。
 */
data class VaultInfo(
    /** 保险箱是否已初始化 */
    val isInitialized: Boolean,
    /** 已注册的信封数量 */
    val envelopeCount: Int,
    /** 可用的认证方式列表 */
    val availableMethods: List<String>,
    /** 是否需要重新生成恢复码 */
    val needsRecoveryRegeneration: Boolean = false
)
