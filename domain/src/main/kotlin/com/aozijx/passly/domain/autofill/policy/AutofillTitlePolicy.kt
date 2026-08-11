package com.aozijx.passly.domain.autofill.policy

/**
 * 自动填充标题生成策略入口：
 * 组合网页、应用和兜底三个子策略，根据场景自动选择。
 */
object AutofillTitlePolicy {

    /**
     * 为 Autofill 保存的新条目生成稳定、可读的标题。
     *
     * 应用场景优先使用系统应用标签；网页场景才考虑窗口标题。
     * 纯数字、UUID、时间戳等窗口内部标识不得成为用户可见标题。
     */
    fun resolveSavedCredentialTitle(
        pageTitle: String?,
        domain: String?,
        appLabel: String?,
        packageName: String?,
        fallback: String
    ): String = when {
        !domain.isNullOrBlank() -> WebTitlePolicy.generate(
            readablePageTitle(pageTitle),
            domain
        )

        !appLabel.isNullOrBlank() || !packageName.isNullOrBlank() ->
            AppTitlePolicy.generate(appLabel, packageName, fallback)

        else -> readablePageTitle(pageTitle) ?: fallback
    }

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

    private fun readablePageTitle(value: String?): String? {
        val title = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
        if (title.none(Char::isLetter)) return null
        if (UUID_PATTERN.matches(title)) return null

        val identifierCharacters = title.count { it.isDigit() || it == '-' || it == '_' }
        val looksLikeOpaqueIdentifier =
            title.length >= 12 && identifierCharacters.toFloat() / title.length >= 0.75f
        return title.takeUnless { looksLikeOpaqueIdentifier }
    }

    private val UUID_PATTERN = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-" +
                "[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    )
}
