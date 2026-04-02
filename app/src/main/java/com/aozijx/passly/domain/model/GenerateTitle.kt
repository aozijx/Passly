package com.aozijx.passly.domain.model

import android.content.Context
import com.aozijx.passly.R
import java.util.Calendar

/**
 * 自动填充标题生成策略
 */
object AutofillTitleGenerator {

    /**
     * 智能生成标题主入口
     */
    fun getSmartTitle(
        context: Context,
        pageTitle: String?,
        domain: String?,
        appLabel: String?,
        packageName: String?
    ): String {
        return when {
            // 1. 如果有域名，认为是网页环境
            domain != null -> generateSmartWebTitle(pageTitle, domain)
            
            // 2. 如果有应用信息，认为是 App 环境
            appLabel != null || packageName != null -> generateSmartAppTitle(context, appLabel, packageName)
            
            // 3. 兜底策略
            else -> generateSmartFallbackTitle(context)
        }
    }

    // 辅助函数：智能生成网页标题
    private fun generateSmartWebTitle(pageTitle: String?, domain: String): String {
        return if (pageTitle != null && pageTitle.isNotBlank() && !pageTitle.contains("/")) {
            when {
                // 标题太长，截取并添加省略号
                pageTitle.length > 30 -> pageTitle.take(27) + "..."

                // 标题包含域名后缀但无意义，清理后使用
                pageTitle.contains(domain, ignoreCase = true) && pageTitle.length < 10 -> {
                    pageTitle.replace(domain, "", ignoreCase = true)
                        .trim()
                        .takeIf { it.isNotBlank() } ?: domain
                }

                // 标题包含特殊字符，提取有意义的部分
                pageTitle.contains(Regex("[-|•·>_]")) -> {
                    // 取第一个有意义的部分
                    pageTitle.split(Regex("[-|•·>_]"))
                        .firstOrNull { it.trim().length > 3 }
                        ?.trim() ?: pageTitle
                }

                else -> pageTitle
            }
        } else {
            // 域名处理：去掉 www. 并去除常见后缀
            domain.removePrefix("www.").removeCommonDomainSuffix()
        }
    }

    // 辅助函数：智能生成应用标题
    private fun generateSmartAppTitle(context: Context, appLabel: String?, packageName: String?): String {
        val appFallback = context.getString(R.string.autofill_title_app_fallback)
        if (appLabel != null && appLabel.isNotBlank()) {
            return when {
                appLabel.length > 20 -> appLabel.take(18) + "..."
                appLabel.contains(Regex("[\\u4e00-\\u9fa5]")) -> appLabel
                else -> cleanAppName(appLabel)
            }
        }

        if (packageName != null) {
            val segments = packageName.split('.')
            return when {
                segments.size >= 3 -> {
                    val lastTwo = segments.takeLast(2)
                    if (lastTwo.any { it == "ui" || it == "activity" || it == "view" }) {
                        segments.takeLast(3).firstOrNull()?.replaceFirstChar { it.uppercase() } ?: appFallback
                    } else {
                        lastTwo.lastOrNull()?.replaceFirstChar { it.uppercase() } ?: appFallback
                    }
                }
                segments.size == 2 -> segments.last().replaceFirstChar { it.uppercase() }
                else -> segments.lastOrNull()?.replaceFirstChar { it.uppercase() } ?: appFallback
            }
        }

        return generateSmartFallbackTitle(context)
    }

    // 辅助函数：生成智能兜底标题
    private fun generateSmartFallbackTitle(context: Context): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..5 -> context.getString(R.string.autofill_title_late_night)
            in 6..11 -> context.getString(R.string.autofill_title_morning)
            in 12..13 -> context.getString(R.string.autofill_title_noon)
            in 14..17 -> context.getString(R.string.autofill_title_afternoon)
            in 18..21 -> context.getString(R.string.autofill_title_evening)
            else -> context.getString(R.string.autofill_title_new_entry)
        }
    }

    private fun String.removeCommonDomainSuffix(): String {
        val commonSuffixes = listOf(
            ".com", ".cn", ".net", ".org", ".edu", ".gov",
            ".io", ".ai", ".app", ".dev", ".tech",
            ".co.uk", ".com.cn", ".net.cn"
        )
        var result = this
        for (suffix in commonSuffixes) {
            if (result.endsWith(suffix, ignoreCase = true)) {
                result = result.removeSuffix(suffix)
                break
            }
        }
        return if (result.isBlank() || result.length < 2) this else result
    }

    private fun cleanAppName(appName: String): String {
        val commonSuffixes = listOf("App", "Application", "Android", "Mobile", " - ", " – ", " | ")
        var cleaned = appName
        for (suffix in commonSuffixes) {
            cleaned = cleaned.replace(suffix, " ", ignoreCase = true)
        }
        return cleaned.trim()
    }
}
