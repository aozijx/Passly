package com.aozijx.passly.security.crypto

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 加密引擎 —— 业务层唯一的加密入口。
 *
 * 通过 [DekManager.withDek] 获取 DEK，执行加密/解密操作后立即释放。
 * 业务层（Repository 等）只接触 CryptoEngine，永远不直接持有 DEK。
 *
 * [withUnlockedDek] 保证：已解锁 → 自动 wipe → 自动异常处理。
 */
@Singleton
class CryptoEngine @Inject constructor(
    private val dekManager: DekManager
) {
    /**
     * 在 DEK 作用域内执行操作。
     *
     * 仅用于需要原始 DEK 字节的场景（如 SQLCipher 数据库打开），
     * 常规加密/解密应使用 [encrypt] / [decrypt] 方法。
     */
    suspend fun <T> withUnlockedDek(block: (ByteArray) -> T): T = dekManager.withDek(block)
}
