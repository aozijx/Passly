package com.aozijx.passly.domain.diagnostics.repository

data class DatabaseQuarantineResult(
    val recoveryId: String? = null,
    val error: Throwable? = null
)

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
     * 保留当前数据库及关联文件的私有恢复副本，然后创建空数据库。
     *
     * @return 恢复包编号；没有可保留文件时为 null。失败时抛出异常。
     */
    suspend fun quarantineAndReinitialize(): DatabaseQuarantineResult

    /**
     * 删除无法打开的数据库及其关联加密存储文件，并创建空数据库。
     *
     * 保留独立存储的设置、认证信封和密钥绑定。
     */
    suspend fun clearAndReinitialize(): Throwable?

    /**
     * 关闭底层数据库连接，确保资源释放完毕后返回。
     */
    suspend fun close()
}
