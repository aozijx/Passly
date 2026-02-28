package com.example.poop.ui.screens.login

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.poop.ui.theme.PoopTheme
import com.example.poop.data.Preference

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 开启边到边显示，并让系统自动管理状态栏图标颜色
        enableEdgeToEdge()

        setContent {
            // 1. 实例化你的 Preference 类
            val preference = remember { Preference(applicationContext) }
            // initialValue 建议使用系统默认值，这样启动时不会有闪烁
            val isDarkModePref by preference.isDarkMode.collectAsState(initial = null)
            val isDynamicColorPref by preference.isDynamicColor.collectAsState(initial = true)

            PoopTheme(
                darkTheme = isDarkModePref,
                dynamicColor = isDynamicColorPref
            ) {
                LoginScreen(onBack = { finish() })
            }
        }
    }
}
