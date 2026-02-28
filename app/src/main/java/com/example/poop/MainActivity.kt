package com.example.poop

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.poop.ui.navigation.NavGraph
import com.example.poop.ui.navigation.Screen
import com.example.poop.ui.theme.PoopTheme
import com.example.poop.data.Preference

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 开启 FLAG_SECURE 保护，防止该页面及后续页面被截屏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        
        enableEdgeToEdge()
        checkAndRequestPermissions()

        setContent {
            // 1. 实例化你的 Preference 类
            val preference = remember { Preference(applicationContext) }
            // initialValue 建议使用系统默认值，这样启动时不会有闪烁
            val isDarkModePref by preference.isDarkMode.collectAsState(initial = null)

            PoopTheme(darkTheme = if (isDarkModePref == true) true else null) {
                // 直接调用 NavGraph，并指定起始页
                NavGraph(startDestination = Screen.Home.route)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1001)
        }
    }
}
