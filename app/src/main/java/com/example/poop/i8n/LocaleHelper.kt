package com.example.poop.i8n

import android.content.Context
import android.content.res.XmlResourceParser
import androidx.annotation.XmlRes
import com.example.poop.R
import java.util.Locale

data class LanguageOption(
    val tag: String,         // "" = 系统默认 / 跟随系统
    val displayName: String  // 用户可见的友好名称
)

object LocaleConfigReader {
    private var cachedRawTags: List<String>? = null
    private var cachedOptions: List<LanguageOption>? = null
    private var lastDisplayLocale: Locale? = null

    fun getSupportedLanguages(
        context: Context,
        @XmlRes localesConfigResId: Int = R.xml.local_config
    ): List<LanguageOption> {
        val currentLocale = Locale.getDefault()

        // 只有当显示语言改变时，才重新生成带翻译的名称
        if (cachedRawTags != null && lastDisplayLocale == currentLocale) {
            return cachedOptions!!
        }

        val tags = cachedRawTags ?: parseLocalesConfig(context, localesConfigResId)
        cachedRawTags = tags

        val options = tags.map { tag ->
            val displayName = if (tag.isEmpty()) {
                context.getString(R.string.follow_system)
            } else {
                getNiceDisplayName(Locale.forLanguageTag(tag), currentLocale)
            }
            LanguageOption(tag, displayName)
        }

        val sorted = options.sortedWith(compareBy { it.tag.isNotEmpty() })
        cachedOptions = sorted
        lastDisplayLocale = currentLocale
        return sorted
    }

    private fun parseLocalesConfig(context: Context, resId: Int): List<String> {
        val tags = mutableListOf<String>()
        try {
            context.resources.getXml(resId).use { parser ->
                var eventType = parser.eventType
                while (eventType != XmlResourceParser.END_DOCUMENT) {
                    if (eventType == XmlResourceParser.START_TAG && parser.name == "locale") {
                        // 同时尝试获取带命名空间和不带命名空间的属性
                        val name = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")
                            ?: parser.getAttributeValue(null, "name")
                        name?.let { tags.add(it.trim()) }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            // 失败时返回基础默认值
            return listOf("", "en", "zh")
        }
        return if (tags.isEmpty()) listOf("", "en", "zh") else tags
    }

    private fun getNiceDisplayName(locale: Locale, displayLocale: Locale): String {
        return locale.getDisplayName(displayLocale).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(displayLocale) else it.toString()
        }
    }
}
