package com.aozijx.passly.domain.autofill.policy

/**
 * 域名标准化工具：
 * 统一自动填充匹配、凭据关联等场景对原始域名的清洗规则，
 * 避免在 data / service 多处重复实现导致匹配行为漂移。
 */
object DomainNameNormalizer {
    private val commonSuffixes = listOf(
        ".com", ".cn", ".net", ".org", ".edu", ".gov",
        ".io", ".ai", ".app", ".dev", ".tech",
        ".co.uk", ".com.cn", ".net.cn"
    )

    /**
     * 标准化域名
     * @param raw 原始域名或 URL
     * @return 标准化后的域名，如果输入无效则返回 null
     */
    fun normalize(raw: String?): String? {
        val value = raw?.trim()
            ?.lowercase()
            ?.removePrefix("https://")
            ?.removePrefix("http://")
            ?.substringBefore('/')
            ?.removePrefix("www.")
        return value?.takeIf { it.isNotBlank() }
    }

    /**
     * 移除常见域名后缀
     * @param domain 域名
     * @return 移除后缀后的域名，如果结果过短则返回原域名
     */
    fun removeCommonDomainSuffix(domain: String): String {
        var result = domain
        for (suffix in commonSuffixes) {
            if (result.endsWith(suffix, ignoreCase = true)) {
                result = result.dropLast(suffix.length)
                break
            }
        }
        return if (result.isBlank() || result.length < 2) domain else result
    }
}
