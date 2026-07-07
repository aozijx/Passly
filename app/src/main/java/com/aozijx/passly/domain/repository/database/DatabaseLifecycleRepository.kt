package com.aozijx.passly.domain.repository.database

/**
 * 数据库生命周期仓库接口。
 * 负责屏蔽 features 层对底层 AppDatabase 单例的直接访问，
 * 由数据层掌控预热、重试与关闭语义。
 */
interface DatabaseLifecycleRepository {
    /**
     * 预热数据库并探测可用性。
     * @return 若初始化过程中出现错误则返回 Throwable；成功则返回 null。
     */
    suspend fun preWarm(): Throwable?

    /**
     * 重置并重建数据库连接，清除残留的初始化错误。
     * 主要用于初始化失败后由用户显式触发的重试。
     * @return 重建后是否仍存在错误。
     */
    suspend fun retry(): Throwable?

    /**
     * 返回并清空一次性的数据库自动恢复提示信息。
     */
    fun consumeAutoRecoveryNotice(): String?

    /**
     * 关闭底层数据库连接，确保资源释放完毕后返回。
     */
    suspend fun close()
}