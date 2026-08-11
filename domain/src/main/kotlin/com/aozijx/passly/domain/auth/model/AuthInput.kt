package com.aozijx.passly.domain.auth.model

/**
 * 认证输入凭据。
 *
 * 替代可空的 [CharArray] 统一表达三种场景：
 * - [Interactive] — 交互式认证（认证器自行从用户获取凭据）
 * - [AppPassword] — 应用密码（已有输入值）
 * - [RecoveryCode] — 恢复码（已有输入值）
 *
 * 每个 [OwnedChars] 确保使用后擦除，且所有权明确。
 */
sealed interface AuthInput {
    /** Transfers credential ownership to the authorization boundary. */
    fun consumeCredential(): CharArray?

    /** 交互式认证 — 认证器自行与用户交互获取凭据 */
    data object Interactive : AuthInput {
        override fun consumeCredential(): CharArray? = null
    }

    /** 应用密码 — 调用方已获取明文密码 */
    class AppPassword internal constructor(
        internal val secret: OwnedChars
    ) : AuthInput {
        override fun consumeCredential(): CharArray = secret.consume()

        companion object {
            fun from(chars: CharArray): AppPassword = AppPassword(OwnedChars.take(chars))
        }
    }

    /** 恢复码 — 调用方已获取恢复码 */
    class RecoveryCode internal constructor(
        internal val secret: OwnedChars
    ) : AuthInput {
        override fun consumeCredential(): CharArray = secret.consume()

        companion object {
            fun from(chars: CharArray): RecoveryCode = RecoveryCode(OwnedChars.take(chars))
        }
    }
}
