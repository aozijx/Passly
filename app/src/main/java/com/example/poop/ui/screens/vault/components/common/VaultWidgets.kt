package com.example.poop.ui.screens.vault.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.poop.data.VaultEntry

/**
 * 集中管理所有可选图标
 */
object VaultIcons {
    val allIcons = mapOf(
        "银行" to Icons.Default.AccountBalance,
        "信用卡" to Icons.Default.CreditCard,
        "钱包" to Icons.Default.AccountBalanceWallet,
        "账单" to Icons.Default.Payments,
        "储蓄" to Icons.Default.Savings,
        "理财" to Icons.AutoMirrored.Filled.TrendingUp,
        "个人" to Icons.Default.Person,
        "隐私" to Icons.Default.Fingerprint,
        "社交" to Icons.Default.Forum,
        "群组" to Icons.Default.Groups,
        "证件" to Icons.Default.Badge,
        "密钥" to Icons.Default.VpnKey,
        "邮件" to Icons.Default.Email,
        "账号" to Icons.Default.AlternateEmail,
        "游戏" to Icons.Default.SportsEsports,
        "视频" to Icons.Default.Subscriptions,
        "影视" to Icons.Default.Movie,
        "购物车" to Icons.Default.ShoppingCart,
        "购物袋" to Icons.Default.ShoppingBag,
        "直播" to Icons.Default.LiveTv,
        "摄像" to Icons.Default.Videocam,
        "收藏" to Icons.Default.Star,
        "心仪" to Icons.Default.Favorite,
        "医疗" to Icons.Default.HealthAndSafety,
        "健康" to Icons.Default.MedicalServices,
        "云端" to Icons.Default.Cloud,
        "笔记" to Icons.Default.EditNote,
        "日记" to Icons.Default.Book,
        "高铁" to Icons.Default.Train,
        "飞机" to Icons.Default.Flight,
        "校园" to Icons.Default.School,
        "工作" to Icons.Default.Work,
        "代码" to Icons.Default.Terminal,
        "网页" to Icons.Default.Language,
        "WiFi" to Icons.Default.Wifi,
        "默认" to Icons.Default.Key,
        "锁定" to Icons.Default.Lock,
        "防护" to Icons.Default.Shield,
        "应用" to Icons.Default.Apps,
    )

    fun getIconByName(name: String?): ImageVector {
        return allIcons[name] ?: Icons.Default.Key
    }
}

/**
 * 统一图标展示组件
 */
@Composable
fun VaultItemIcon(
    item: VaultEntry,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    if (!item.iconCustomPath.isNullOrEmpty()) {
        AsyncImage(
            model = item.iconCustomPath,
            contentDescription = null,
            modifier = modifier.size(36.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val icon = if (!item.iconName.isNullOrEmpty()) {
            VaultIcons.getIconByName(item.iconName)
        } else {
            getCategoryIcon(item.category)
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = modifier.size(24.dp)
        )
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

fun getCategoryIcon(category: String): ImageVector = when (category.lowercase()) {
    "个人", "private", "隐私" -> Icons.Default.Fingerprint
    "银行卡", "bank card", "金融", "finance", "网银", "银行" -> Icons.Default.AccountBalance
    "支付", "payment", "充值", "recharge", "转账" -> Icons.Default.Payments
    "提现", "withdraw", "理财", "financial management", "存钱" -> Icons.AutoMirrored.Filled.TrendingUp
    "账号", "account", "登录", "login" -> Icons.Default.VpnKey
    "社交", "social", "聊天", "沟通", "微信", "qq" -> Icons.Default.Forum
    "邮箱", "email", "邮件", "mail" -> Icons.Default.Email
    "应用", "APP", "app", "application" -> Icons.Default.Apps
    "游戏", "game" -> Icons.Default.SportsEsports
    "视频", "video", "会员", "影音", "movie", "影视" -> Icons.Default.Subscriptions
    "购物", "shopping", "电商", "淘宝", "京东" -> Icons.Default.ShoppingCart
    "直播", "live", "主播" -> Icons.Default.LiveTv
    "公积金", "社保", "医保", "健康", "health", "medical" -> Icons.Default.HealthAndSafety
    "网盘", "cloud", "云盘", "drive" -> Icons.Default.Cloud
    "笔记", "note", "记事", "日记", "diary", "书" -> Icons.Default.EditNote
    "工作", "work", "打工" -> Icons.Default.Work
    "学习", "school", "教育", "校园" -> Icons.Default.School
    "出行", "travel", "高铁", "火车" -> Icons.Default.Train
    "飞机", "飞行", "flight" -> Icons.Default.Flight
    "wifi", "网络", "无线" -> Icons.Default.Wifi
    "安全", "security", "权限", "permission" -> Icons.Default.Security
    else -> Icons.Default.Key
}
