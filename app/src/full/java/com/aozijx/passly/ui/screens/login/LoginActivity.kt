package com.aozijx.passly.ui.screens.login

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.aozijx.passly.data.AppPreference
import com.aozijx.passly.ui.theme.AppTheme
import com.aozijx.passly.ui.screens.login.LoginScreen

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 开启边到边显示，并让系统自动管理状态栏图标颜色
        enableEdgeToEdge()

        setContent {
            // 1. 实例化 Preference 类
            val preference = remember { AppPreference(applicationContext) }
            // initialValue 建议使用系统默认值，这样启动时不会有闪烁
            val isDarkModePref by preference.isDarkMode.collectAsState(initial = null)
            val isDynamicColorPref by preference.isDynamicColor.collectAsState(initial = true)

            AppTheme(
                darkTheme = if (isDarkModePref == true) true else null,
                dynamicColor = isDynamicColorPref
            ) {
                LoginScreen(onBack = { finish() })
            }
        }
    }
}

