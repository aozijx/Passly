package com.aozijx.passly.domain.autofill.policy

import com.aozijx.passly.core.normalization.DomainNormalizer

/**
 * 网页标题生成策略：
 * 从网页标题和域名生成智能的自动填充标题。
 */
object WebTitlePolicy {
    /**
     * 生成智能网页标题
     * @param pageTitle 网页标题
     * @param domain 域名
     * @return 生成的标题
     */
    fun generate(pageTitle: String?, domain: String): String {
        return if (pageTitle != null && pageTitle.isNotBlank() && !pageTitle.contains("/")) {
            when {
                pageTitle.length > 30 -> pageTitle.take(27) + "..."
                pageTitle.contains(domain, ignoreCase = true) && pageTitle.length < 10 -> {
                    pageTitle.replace(domain, "", ignoreCase = true)
                        .trim()
                        .takeIf { it.isNotBlank() } ?: domain
                }

                pageTitle.contains(Regex("[-|•·>_]")) -> {
                    pageTitle.split(Regex("[-|•·>_]"))
                        .firstOrNull { it.trim().length > 3 }
                        ?.trim() ?: pageTitle
                }

                else -> pageTitle
            }
        } else {
            domain.removePrefix("www.").let { DomainNormalizer.removeCommonDomainSuffix(it) }
        }
    }
}
