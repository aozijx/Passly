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

    private var cachedOptions: List<LanguageOption>? = null

    /**
     * 获取支持的语言列表
     * @param context 用于读取资源和字符串
     * @param localesConfigResId 默认使用 R.xml.local_config
     * @return 排序后的 LanguageOption 列表
     */
    fun getSupportedLanguages(
        context: Context,
        @XmlRes localesConfigResId: Int = R.xml.local_config
    ): List<LanguageOption> {
        cachedOptions?.let { return it }

        val options = mutableListOf<LanguageOption>()
        
        // 核心逻辑：从 res/xml/local_config.xml 解析
        try {
            val parser: XmlResourceParser = context.resources.getXml(localesConfigResId)
            var eventType = parser.eventType

            while (eventType != XmlResourceParser.END_DOCUMENT) {
                if (eventType == XmlResourceParser.START_TAG && parser.name == "locale") {
                    // 读取 android:name 属性
                    val name = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")?.trim() ?: ""
                    
                    val displayName = if (name.isEmpty()) {
                        context.getString(R.string.follow_system)
                    } else {
                        val locale = Locale.forLanguageTag(name)
                        getNiceDisplayName(locale, name)
                    }
                    
                    if (options.none { it.tag == name }) {
                        options.add(LanguageOption(name, displayName))
                    }
                }
                eventType = parser.next()
            }
            parser.close()
        } catch (e: Exception) {
            // Fallback
            if (options.isEmpty()) {
                options.add(LanguageOption("", context.getString(R.string.follow_system)))
                options.add(LanguageOption("en", "English"))
                options.add(LanguageOption("zh", "中文 (简体)"))
            }
        }

        // 排序：空 tag (跟随系统) 置顶
        val sorted = options.sortedWith(compareBy { it.tag.isNotEmpty() })

        cachedOptions = sorted
        return sorted
    }

    private fun getNiceDisplayName(locale: Locale, tag: String): String {
        return when (tag) {
            "zh", "zh-CN", "zh-Hans" -> "中文 (简体)"
            "zh-TW", "zh-HK", "zh-Hant" -> "中文 (繁體)"
            "en" -> "English"
            "ja" -> "日本語"
            else -> {
                // 自适应显示：如果是该语言本身，则首字母大写
                locale.getDisplayName(locale).replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase(locale) else it.toString() 
                }
            }
        }
    }
}
