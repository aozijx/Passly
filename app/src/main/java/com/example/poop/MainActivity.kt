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
import com.example.poop.ui.screens.HomeScreen
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
private fun AppContent() {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        HomeScreen(
            modifier = Modifier
                .padding(paddingValues)
        )
//        DetailScreen(
//            modifier = Modifier
//                .padding(paddingValues)
//        )
    }
}

