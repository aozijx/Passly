package com.aozijx.passly.core.session

/**
 * 数据库访问闸门状态。
 *
 * 状态转换：
 * ```
 * SEALED → UNLOCKED: 需要 DEK 打开数据库
 * SOFT_LOCKED → UNLOCKED: 数据库保持打开，仅更改状态
 * UNLOCKED → SOFT_LOCKED: 阻止新 lease，数据库保持打开
 * UNLOCKED → SEALED: 排干 lease、关闭数据库
 * ```
 *
 * ## 语义说明
 * - **SOFT_LOCKED**：应用层访问控制，SQLCipher 连接仍保持打开。
 *   原生连接内部仍掌握数据库解密能力，不是完整的密码学封存。
 * - **SEALED**：完整封存，关闭数据库连接并擦除密钥。
 */
enum class LockState {
    SOFT_LOCKED,
    SEALED,
    UNLOCKED
}
