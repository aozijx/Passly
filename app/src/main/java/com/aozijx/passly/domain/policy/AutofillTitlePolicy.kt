package com.aozijx.passly.domain.policy

import java.util.Calendar

/**
 * 纯业务的自动填充标题生成策略（不依赖 Android）。
 */
object AutofillTitlePolicy {

    data class AutofillTitleStrings(
        val appFallback: String,
        val lateNight: String,
        val morning: String,
        val noon: String,
        val afternoon: String,
        val evening: String,
        val newEntry: String
    )

    fun getSmartTitle(
        pageTitle: String?,
        domain: String?,
        appLabel: String?,
        packageName: String?,
        strings: AutofillTitleStrings,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        return when {
            domain != null -> generateSmartWebTitle(pageTitle, domain)
            appLabel != null || packageName != null -> generateSmartAppTitle(appLabel, packageName, strings.appFallback)
            else -> generateSmartFallbackTitle(strings, nowMillis)
        }
    }

    private fun generateSmartWebTitle(pageTitle: String?, domain: String): String {
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
            domain.removePrefix("www.").removeCommonDomainSuffix()
        }
    }

    private fun generateSmartAppTitle(appLabel: String?, packageName: String?, appFallback: String): String {
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

        return appFallback
    }

    private fun generateSmartFallbackTitle(strings: AutofillTitleStrings, nowMillis: Long): String {
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
