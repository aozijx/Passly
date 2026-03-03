package com.example.poop.ui.screens.vault.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.poop.data.VaultItem

/**
 * 集中管理所有可选图标
 */
object VaultIcons {
    val allIcons = mapOf(
        "Fingerprint" to Icons.Default.Fingerprint,
        "CreditCard" to Icons.Default.CreditCard,
        "Wallet" to Icons.Default.AccountBalanceWallet,
        "Account" to Icons.Default.AlternateEmail,
        "App" to Icons.Default.Apps,
        "Email" to Icons.Default.Email,
        "Security" to Icons.Default.Security,
        "Savings" to Icons.Default.Savings,
        "Medical" to Icons.Default.MedicalInformation,
        "Badge" to Icons.Default.Badge,
        "Wifi" to Icons.Default.Wifi,
        "Game" to Icons.Default.SportsEsports,
        "Cloud" to Icons.Default.Cloud,
        "Note" to Icons.Default.EditNote,
        "Key" to Icons.Default.Key,
        "Lock" to Icons.Default.Lock,
        "Shopping" to Icons.Default.ShoppingCart,
        "Car" to Icons.Default.DirectionsCar,
        "Home" to Icons.Default.Home,
        "Work" to Icons.Default.Work,
        "Flight" to Icons.Default.Flight,
        "Star" to Icons.Default.Star,
        "Heart" to Icons.Default.Favorite,
        "Shield" to Icons.Default.Shield,
        "Call" to Icons.Default.Call,
        "Language" to Icons.Default.Language,
        "Terminal" to Icons.Default.Terminal,
        "Public" to Icons.Default.Public,
        "School" to Icons.Default.School,
        "Person" to Icons.Default.Person
    )

    /**
     * 获取指定名称的图标，找不到则返回 Key 默认图标
     */
    fun getIconByName(name: String?): ImageVector {
        return allIcons[name] ?: Icons.Default.Key
    }
}

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

/**
 * 根据 VaultItem 获取图标逻辑：
 * 优先级：自定义 iconName > category 映射 > 默认图标
 */
fun getVaultItemIcon(item: VaultItem): ImageVector {
    // 1. 如果有自定义图标，优先使用
    if (item.iconName != null) {
        return VaultIcons.getIconByName(item.iconName)
    }
    
    // 2. 否则按分类自动映射
    return getCategoryIcon(item.category)
}

fun getCategoryIcon(category: String): ImageVector = when (category.lowercase()) {
    "个人", "private" -> Icons.Default.Fingerprint
    "银行卡", "bank card", "金融", "finance" -> Icons.Default.CreditCard
    "支付", "payment", "充值", "recharge" -> Icons.Default.AccountBalanceWallet
    "账号", "account", "登录", "login" -> Icons.Default.AlternateEmail
    "应用", "APP", "app", "application" -> Icons.Default.Apps
    "邮箱", "email", "邮件", "mail" -> Icons.Default.Email
    "安全", "security", "权限", "permission" -> Icons.Default.Security
    "提现", "withdraw", "理财", "financial management" -> Icons.Default.Savings
    "社保", "social security", "医保", "medical insurance" -> Icons.Default.MedicalInformation
    "身份证", "ID card", "证件", "certificate" -> Icons.Default.Badge
    "WiFi", "网络", "无线" -> Icons.Default.Wifi
    "游戏", "game" -> Icons.Default.SportsEsports
    "网盘", "cloud", "云盘", "drive" -> Icons.Default.Cloud
    "日记", "note", "记事", "diary" -> Icons.Default.EditNote
    else -> Icons.Default.Key
}
