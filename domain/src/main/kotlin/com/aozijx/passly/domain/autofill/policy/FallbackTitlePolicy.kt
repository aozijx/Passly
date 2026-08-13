package com.aozijx.passly.domain.autofill.policy

import java.util.Calendar

/**
 * 兜底标题生成策略：
 * 在没有网页或应用信息时，根据当前时间生成友好的默认标题。
 */
object FallbackTitlePolicy {
    /**
     * 根据当前时间生成智能兜底标题
     * @param strings 标题字符串资源
     * @param nowMillis 当前时间戳（毫秒）
     * @return 生成的标题
     */
    fun generate(strings: Strings, nowMillis: Long = System.currentTimeMillis()): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..5 -> strings.lateNight
            in 6..11 -> strings.morning
            in 12..13 -> strings.noon
            in 14..17 -> strings.afternoon
            in 18..21 -> strings.evening
            else -> strings.newEntry
        }
    }

    /**
     * 标题字符串资源
     */
    data class Strings(
        val lateNight: String,
        val morning: String,
        val noon: String,
        val afternoon: String,
        val evening: String,
        val newEntry: String
    )
}