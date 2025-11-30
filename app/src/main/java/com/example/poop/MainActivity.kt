package com.example.poop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.poop.model.navItems
import com.example.poop.ui.component.SimpleBottomBar
import com.example.poop.ui.screens.home.HomeScreen
import com.example.poop.ui.theme.PoopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PoopTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // 传入MainActivity::class.java，让“首页”导航项选中
            SimpleBottomBar(navItems, currentActivityClass = MainActivity::class.java)
        }
    ) { innerPadding ->
        HomeScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

