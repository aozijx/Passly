package com.aozijx.passly.core.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 管理明文导出的 Token 时效性。
 * 纯内存逻辑，无外部依赖。
 */
@Singleton
class PlainExportTokenManager @Inject constructor() {
    private var plainExportTokenIssuedAt: Long = 0L
    private var plainExportTokenTtlMs: Long = 0L

    fun issueToken(ttlMs: Long = 60_000L) {
        plainExportTokenIssuedAt = System.currentTimeMillis()
        plainExportTokenTtlMs = ttlMs
    }

    fun isTokenValid(): Boolean {
        if (plainExportTokenIssuedAt <= 0L || plainExportTokenTtlMs <= 0L) return false
        return System.currentTimeMillis() <= plainExportTokenIssuedAt + plainExportTokenTtlMs
    }

    fun consumeToken(): Boolean {
        val valid = isTokenValid()
        plainExportTokenIssuedAt = 0L
        plainExportTokenTtlMs = 0L
        return valid
    }
}