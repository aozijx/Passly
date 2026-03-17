package com.example.poop

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.poop.data.AppPreference
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContext : Application() {
    
    // 全局单例 AppPreference，属于 main
    val preference: AppPreference by lazy { AppPreference(this) }

    companion object {
        private var _instance: AppContext? = null
        fun get(): AppContext = _instance!!
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this

        // 启动时应用保存的语言设置
        MainScope().launch {
            val language = preference.language.first()
            val appLocale: LocaleListCompat = if (language.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(language)
            }
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }
}
