package com.aozijx.passly.domain.auth.model

import com.aozijx.passly.domain.auth.failure.AuthFailure

/**
 * 解锁操作结果。
 */
sealed interface UnlockResult {
    /** 解锁成功 */
    data object Unlocked : UnlockResult

    /** 用户取消 */
    data object Cancelled : UnlockResult

    /** 拒绝（凭据错误、不可恢复等） */
    data class Denied(val failure: AuthFailure) : UnlockResult
}
