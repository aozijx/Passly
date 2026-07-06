package com.aozijx.passly.domain.autofill.policy

/**
 * 自动填充标题生成策略入口：
 * 组合网页、应用和兜底三个子策略，根据场景自动选择。
 */
object AutofillTitlePolicy {

    /**
     * 标题字符串资源
     */
    data class AutofillTitleStrings(
        val appFallback: String,
        val lateNight: String,
        val morning: String,
        val noon: String,
        val afternoon: String,
        val evening: String,
        val newEntry: String
    )

    /**
     * 生成智能自动填充标题
     * @param pageTitle 网页标题
     * @param domain 域名
     * @param appLabel 应用标签
     * @param packageName 包名
     * @param strings 标题字符串资源
     * @param nowMillis 当前时间戳（毫秒）
     * @return 生成的标题
     */
    fun getSmartTitle(
        pageTitle: String?,
        domain: String?,
        appLabel: String?,
        packageName: String?,
        strings: AutofillTitleStrings,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        return when {
            domain != null -> WebTitlePolicy.generate(pageTitle, domain)
            appLabel != null || packageName != null -> AppTitlePolicy.generate(
                appLabel,
                packageName,
                strings.appFallback
            )

            else -> FallbackTitlePolicy.generate(
                FallbackTitlePolicy.Strings(
                    lateNight = strings.lateNight,
                    morning = strings.morning,
                    noon = strings.noon,
                    afternoon = strings.afternoon,
                    evening = strings.evening,
                    newEntry = strings.newEntry
                ),
                nowMillis
            )
        }
    }
}