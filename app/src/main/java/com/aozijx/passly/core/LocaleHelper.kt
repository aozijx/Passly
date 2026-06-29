package com.aozijx.passly.core

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    fun applyLanguage(context: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "en" -> Locale.ENGLISH
            "ja" -> Locale.JAPANESE
            else ->
                context.resources.configuration.locales[0]
        }

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}