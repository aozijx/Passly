package com.aozijx.passly.domain.authentication

import kotlinx.coroutines.flow.StateFlow

/**
 * 会话状态提供者（Domain 层契约）。
 *
 * Repository 和 UseCase 依赖此接口而非具体数据库实现，便于单元测试时注入永远活跃的 Fake。
 *
 * 当前仅暴露锁状态供 Repository 判断是否可访问；
 * 实际的资源租约管理由 runtime session 模块负责。
 */
interface SessionStateProvider {

    /** 当前数据库锁状态 */
    val lockState: SecureSessionState

    /** 可观察的数据库锁状态，供长生命周期任务及时停止敏感访问。 */
    val lockStateFlow: StateFlow<SecureSessionState>

    /** 会话是否已解锁且可写 */
    val isWritable: Boolean
        get() = lockState == SecureSessionState.UNLOCKED
}
