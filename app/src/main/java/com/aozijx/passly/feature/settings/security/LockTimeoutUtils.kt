package com.aozijx.passly.feature.settings.security

fun formatLockTimeoutText(timeoutMs: Long): String {
    val seconds = (timeoutMs / 1000L).coerceAtLeast(1L)
    return when {
        seconds < 60L -> "$seconds 秒"
        seconds % 60L == 0L -> "${seconds / 60L} 分钟"
        else -> "${seconds / 60L} 分 ${seconds % 60L} 秒"
    }
}