package com.aozijx.passly

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.aozijx.passly.core.theme.AppTheme
import com.aozijx.passly.data.AppPreference
import com.aozijx.passly.ui.navigation.NavGraph
import com.aozijx.passly.ui.navigation.Screen
import com.aozijx.passly.util.PermissionManager
import com.aozijx.passly.util.ShortcutManager
import kotlinx.coroutines.launch

class FullActivity : AppCompatActivity() {
    private val preference by lazy { AppPreference(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 动态初始化快捷方式，适配当前环境包名
        ShortcutManager.init(this)

        // 初始化权限管理器
        PermissionManager.getInstance().init(this) { isGranted ->
            updateNotificationPref(isGranted)
        }

        // 首次进入检查并请求权限
        val isGranted = PermissionManager.getInstance().hasNotificationPermission(this)
        updateNotificationPref(isGranted)

        if (!isGranted) {
            PermissionManager.getInstance().requestNotificationPermission()
        }

        setContent {
            val isDark by preference.isDarkMode.collectAsState(initial = null)
            val isDynamic by preference.isDynamicColor.collectAsState(initial = true)

            AppTheme(darkTheme = if (isDark == true) true else null, dynamicColor = isDynamic) {
                NavGraph(startDestination = Screen.Home.route)
            }
        }
    }

    private fun updateNotificationPref(enabled: Boolean) {
        lifecycleScope.launch { preference.setNotificationsEnabled(enabled) }
    }
}
