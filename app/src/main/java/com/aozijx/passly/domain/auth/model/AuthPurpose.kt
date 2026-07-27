package com.aozijx.passly.domain.auth.model

/**
 * 认证目的。
 *
 * 每个敏感操作对应一个目的，[AuthPolicy] 根据目的决定新鲜度要求、
 * 可用认证方式和 Grant 有效期。
 *
 * 调用者仅指定目的和 correlationId，不覆盖策略决策。
 */
enum class AuthPurpose {
    /** 解锁保险库（首次解锁或封存后恢复） */
    UNLOCK_VAULT,

    /** 重新认证（会话有效但需新鲜验证） */
    REAUTHENTICATE,

    /** 自动填充凭据 */
    AUTOFILL,

    /** 查看敏感信息 */
    REVEAL_SECRET,

    /** 删除条目 */
    DELETE_ENTRY,

    /** 导出备份 */
    BACKUP_EXPORT,

    /** 导入备份 */
    BACKUP_IMPORT,

    /** 管理应用密码 */
    MANAGE_APP_PASSWORD,

    /** 管理恢复码 */
    MANAGE_RECOVERY_CODE,

    /** 更改生物识别策略 */
    CHANGE_BIOMETRIC_POLICY,

    /** 导出诊断信息 */
    EXPORT_DIAGNOSTICS,

    /** 数据库损坏后的破坏性清理 */
    CLEAR_DATABASE,
}
