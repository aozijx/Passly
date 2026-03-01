package com.example.poop.ui.screens.vault.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun EmptyVaultPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "你的保险箱空空如也",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

fun getCategoryIcon(category: String): ImageVector = when (category.lowercase()) {
    "社交", "social" -> Icons.Default.Public
    "银行", "bank", "金融" -> Icons.Default.CreditCard
    "工作", "work" -> Icons.Default.Work
    "个人", "隐私" -> Icons.Default.Lock
    "购物", "shopping" -> Icons.Default.ShoppingCart
    "游戏", "game" -> Icons.Default.Gamepad
    "音乐", "music" -> Icons.Default.MusicNote
    "电影", "movie" -> Icons.Default.Movie
    "阅读", "book" -> Icons.AutoMirrored.Filled.MenuBook
    "健身", "fitness" -> Icons.Default.FitnessCenter
    "旅行", "travel" -> Icons.Default.Flight
    "美食", "food" -> Icons.Default.Restaurant
    "学习", "study" -> Icons.Default.School
    else -> Icons.Default.Description  // 默认图标
}
