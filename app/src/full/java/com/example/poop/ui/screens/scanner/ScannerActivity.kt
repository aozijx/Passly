package com.example.poop.ui.screens.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.poop.ui.theme.PoopTheme


class ScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PoopTheme(darkTheme = true) {
                ScannerScreen()
            }
        }
    }
}