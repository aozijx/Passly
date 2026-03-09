package com.example.poop

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.poop.data.Preference
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContext : Application() {
    
    // 全局单例 Preference，懒加载以提高启动速度
    val preference: Preference by lazy { Preference(this) }

    companion object {
        private var _instance: AppContext? = null
        
        // 使用非空断言，因为 Application 的实例在进程启动时必然存在
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
