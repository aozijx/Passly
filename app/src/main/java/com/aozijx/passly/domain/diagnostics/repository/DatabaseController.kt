package com.aozijx.passly.domain.diagnostics.repository

/**
 * 数据库生命周期控制器接口。
 * 负责预热探测、重试与关闭，不持有业务数据，也不执行破坏性恢复。
 */
interface DatabaseController {
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
     * 关闭底层数据库连接，确保资源释放完毕后返回。
     */
    suspend fun close()
}
