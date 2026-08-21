package com.aozijx.passly.domain.settings.model

import java.util.Locale

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDynamicColor: Boolean = true,
    val fallbackPalette: FallbackPalette = FallbackPalette.BLUE,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val fontFamily: FontFamilyMode = FontFamilyMode.APP_BUNDLED
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class FallbackPalette { BLUE, GREEN, RED, PURPLE, ORANGE, TEAL, PINK }
enum class AppLanguage(val locale: Locale?) {
    SYSTEM(null),
    ZH(Locale.SIMPLIFIED_CHINESE),
    EN(Locale.ENGLISH),
    JA(Locale.JAPANESE);

    /** BCP-47 标签；跟随系统时 AppCompat 需要空标签。 */
    val applicationLocaleTags: String
        get() = locale?.toLanguageTag().orEmpty()

    /** DataStore 使用的稳定值，兼容已经保存的 "system"。 */
    val storageTag: String
        get() = locale?.toLanguageTag() ?: SYSTEM_STORAGE_TAG

    companion object {
        private const val SYSTEM_STORAGE_TAG = "system"

        fun fromLanguageTag(tag: String): AppLanguage {
            if (tag.isBlank() || tag.equals(SYSTEM_STORAGE_TAG, ignoreCase = true)) return SYSTEM

            val requested = Locale.forLanguageTag(tag)
            return entries.firstOrNull { language ->
                val supported = language.locale ?: return@firstOrNull false
                val hasCompatibleRegion =
                    supported.country.isBlank() ||
                        requested.country.isBlank() ||
                        supported.country == requested.country
                supported.toLanguageTag().equals(tag, ignoreCase = true) ||
                    (supported.language == requested.language && hasCompatibleRegion)
            } ?: SYSTEM
        }
    }
}

enum class FontFamilyMode { SYSTEM, APP_BUNDLED }
