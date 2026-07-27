package com.aozijx.passly.domain.autofill.policy

import com.aozijx.passly.core.util.PackageNormalizer

/**
 * 应用标题生成策略：
 * 从应用标签和包名生成智能的自动填充标题。
 */
object AppTitlePolicy {
    /**
     * 生成智能应用标题
     * @param appLabel 应用标签
     * @param packageName 包名
     * @param fallback 默认值
     * @return 生成的标题
     */
    fun generate(appLabel: String?, packageName: String?, fallback: String): String {
        if (appLabel != null && appLabel.isNotBlank()) {
            return when {
                appLabel.length > 20 -> appLabel.take(18) + "..."
                appLabel.contains(Regex("[\\u4e00-\\u9fa5]")) -> appLabel
                else -> PackageNormalizer.cleanAppName(appLabel)
            }
        }

        return PackageNormalizer.extractReadableName(packageName, fallback)
    }
}