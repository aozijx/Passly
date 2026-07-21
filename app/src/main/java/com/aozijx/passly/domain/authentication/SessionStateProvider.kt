package com.aozijx.passly.domain.authentication

/**
 * 会话状态提供者（Domain 层契约）。
 *
 * Repository 和 UseCase 依赖此接口而非 [com.aozijx.passly.core.session.UnifiedSessionManager]，
 * 实现依赖倒置，便于单元测试时注入永远活跃的 Fake 实现。
 *
 * 职责：
 * - [assertWritable]：检查当前会话是否可写，不可写时抛出 [SessionLockedException]
 * - [trackReadOperation]：包装读操作的引用计数，确保 [lock] 排干时能感知活跃读操作
 */
interface SessionStateProvider {

    /**
     * 检查当前会话是否可写。
     * 若会话已锁定或正在锁定，抛出 [SessionLockedException]。
     */
    fun assertWritable()

    /**
     * 包裹读操作的引用计数。
     *
     * 在 [block] 执行前递增计数，执行后（或异常时）递减计数。
     * 这样 [lock] 中的排干逻辑能等待所有正在执行的读操作完成。
     */
    fun <T> trackReadOperation(block: () -> T): T
}
