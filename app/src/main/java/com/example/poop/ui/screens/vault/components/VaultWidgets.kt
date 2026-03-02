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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Wifi
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
    else -> Icons.Default.Key  // 默认图标
}
