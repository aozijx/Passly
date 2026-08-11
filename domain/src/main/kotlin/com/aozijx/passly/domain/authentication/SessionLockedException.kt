package com.aozijx.passly.domain.authentication

/**
 * 会话已锁定异常，由 [DatabaseLeaseGate] 在租约获取时抛出。
 * 上层（UI 层）可捕获此异常并触发解锁弹窗。
 */
class SessionLockedException(message: String = "Secure session is locked") :
    IllegalStateException(message)
