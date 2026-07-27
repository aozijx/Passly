package com.aozijx.passly.domain.auth.model

/**
 * 单调时钟接口。
 *
 * 使用 [System.nanoTime] 或 [android.os.SystemClock.elapsedRealtime] 实现，
 * 避免 [System.currentTimeMillis] 因系统时间回拨导致的异常。
 */
fun interface MonotonicClock {
    fun elapsedMs(): Long
}
