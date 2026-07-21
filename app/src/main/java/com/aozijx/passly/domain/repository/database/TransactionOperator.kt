package com.aozijx.passly.domain.repository.database

/**
 * 事务操作符（Domain 层契约）。
 *
 * 支持事务嵌套与传播语义：
 * - [withTransaction]：如果当前已有事务则加入，否则新建（默认传播行为）
 * - [withNewTransaction]：强制开启新事务，与外部事务隔离
 *
 * 典型使用场景：
 * - "创建条目 + 记录活动日志" 需要跨 Repository 的事务一致性
 * - 单元测试时可注入 Fake 实现，无需真实的数据库事务
 */
interface TransactionOperator {

    /**
     * 如果当前已有事务则加入，否则新建。
     * 适用于大多数写入场景，自动管理事务传播。
     */
    suspend fun <T> withTransaction(block: suspend () -> T): T

    /**
     * 强制开启新事务，挂起当前事务（若存在）。
     * 适用于需要独立提交/回滚的内部操作，如审计日志记录。
     */
    suspend fun <T> withNewTransaction(block: suspend () -> T): T
}
