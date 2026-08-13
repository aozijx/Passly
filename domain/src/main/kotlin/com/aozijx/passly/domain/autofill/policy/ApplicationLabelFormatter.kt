package com.aozijx.passly.domain.autofill.policy

/**
 * Android 包名展示名格式化：
 * 从 Android 包名中提取有意义的名称，
 * 用于自动填充场景下的标题生成。
 */
object ApplicationLabelFormatter {
    /**
     * 从包名中提取可读名称
     * @param packageName Android 包名（如 com.example.app）
     * @param fallback 如果无法提取时的默认值
     * @return 提取的名称（首字母大写）
     */
    fun format(packageName: String?, fallback: String): String {
        val value = packageName?.trim()
        if (value.isNullOrBlank()) return fallback

        val segments = value.split('.').filter(String::isNotBlank)
        return when {
            segments.size >= 3 -> {
                val lastTwo = segments.takeLast(2)
                if (lastTwo.any { it == "ui" || it == "activity" || it == "view" }) {
                    segments.takeLast(3).firstOrNull()?.replaceFirstChar { it.uppercase() }
                        ?: fallback
                } else {
                    lastTwo.lastOrNull()?.replaceFirstChar { it.uppercase() } ?: fallback
                }
            }

            segments.size == 2 -> segments.last().replaceFirstChar { it.uppercase() }
            else -> segments.lastOrNull()?.replaceFirstChar { it.uppercase() } ?: fallback
        }
    }
}
