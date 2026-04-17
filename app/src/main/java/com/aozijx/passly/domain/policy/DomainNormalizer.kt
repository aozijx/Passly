package com.aozijx.passly.domain.policy

/**
 * 域名归一化工具：
 * 统一自动填充匹配、凭据关联等场景对原始域名的清洗规则，
 * 避免在 data / service 多处重复实现导致匹配行为漂移。
 */
object DomainNormalizer {
    fun normalize(raw: String?): String? {
        val value = raw?.trim()
            ?.lowercase()
            ?.removePrefix("https://")
            ?.removePrefix("http://")
            ?.substringBefore('/')
            ?.removePrefix("www.")
        return value?.takeIf { it.isNotBlank() }
    }
}
