package com.aozijx.passly.security.envelope

/**
 * 信封持久化存储接口。
 *
 * 所有信封通过此接口读写。不同实现可以支持不同的存储后端：
 * - SharedPreferences（当前实现，简单可靠）
 * - EncryptedSharedPreferences（未来升级，信息安全）
 * - DataStore（未来升级，异步安全）
 * - 云端同步（未来扩展）
 */
interface EnvelopeStore {
    /**
     * 保存信封。
     * - 如果同 ID 的信封已存在，覆盖。
     * - 事务中：变更暂存，commit 后统一落盘。
     */
    fun save(envelope: Envelope)

    /**
     * 获取指定 ID 的信封，不存在时返回 null。
     * - 事务中：返回事务开始时的快照，不受未提交变更影响。
     */
    fun get(id: String): Envelope?

    /**
     * 删除指定 ID 的信封。
     * - 事务中：标记删除，commit 后生效。
     */
    fun remove(id: String)

    /**
     * 获取所有信封 ID 集合。
     */
    fun getAllIds(): Set<String>

    /**
     * 是否存在任何信封。
     */
    fun hasAny(): Boolean

    /**
     * 删除所有信封。
     */
    fun clearAll()

    // ─────────────────────────────────────────────────────────
    //  事务
    // ─────────────────────────────────────────────────────────

    /**
     * 开始事务：后续 save/remove 操作暂存，不立即落盘。
     * - 不支持嵌套，重复调用为幂等操作。
     */
    fun beginTransaction()

    /**
     * 提交事务：将所有暂存操作一次性持久化。
     * @throws IllegalStateException 如果未在事务中
     */
    fun commit()

    /**
     * 回滚事务：丢弃所有暂存操作，恢复到事务开始前状态。
     * @throws IllegalStateException 如果未在事务中
     */
    fun rollback()
}
