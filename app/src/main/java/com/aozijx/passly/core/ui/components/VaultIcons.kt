package com.aozijx.passly.core.ui.components

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
import androidx.compose.ui.graphics.vector.ImageVector
import com.aozijx.passly.R

object VaultIcons {
    val allIcons = mapOf(
        R.string.icon_bank to Icons.Default.AccountBalance,
        R.string.icon_card to Icons.Default.CreditCard,
        R.string.icon_wallet to Icons.Default.AccountBalanceWallet,
        R.string.icon_bill to Icons.Default.Payments,
        R.string.icon_savings to Icons.Default.Savings,
        R.string.icon_trending to Icons.AutoMirrored.Filled.TrendingUp,
        R.string.icon_person to Icons.Default.Person,
        R.string.icon_privacy to Icons.Default.Fingerprint,
        R.string.icon_social to Icons.Default.Forum,
        R.string.icon_groups to Icons.Default.Groups,
        R.string.icon_badge to Icons.Default.Badge,
        R.string.icon_key to Icons.Default.VpnKey,
        R.string.icon_email to Icons.Default.Email,
        R.string.username to Icons.Default.AlternateEmail,
        R.string.icon_game to Icons.Default.SportsEsports,
        R.string.icon_video to Icons.Default.Subscriptions,
        R.string.icon_movie to Icons.Default.Movie,
        R.string.icon_shopping to Icons.Default.ShoppingCart,
        R.string.icon_bag to Icons.Default.ShoppingBag,
        R.string.icon_live to Icons.Default.LiveTv,
        R.string.icon_camera to Icons.Default.Videocam,
        R.string.icon_star to Icons.Default.Star,
        R.string.icon_heart to Icons.Default.Favorite,
        R.string.icon_medical to Icons.Default.HealthAndSafety,
        R.string.icon_health to Icons.Default.MedicalServices,
        R.string.icon_cloud to Icons.Default.Cloud,
        R.string.icon_note to Icons.Default.EditNote,
        R.string.icon_book to Icons.Default.Book,
        R.string.icon_train to Icons.Default.Train,
        R.string.icon_flight to Icons.Default.Flight,
        R.string.icon_school to Icons.Default.School,
        R.string.icon_work to Icons.Default.Work,
        R.string.icon_code to Icons.Default.Terminal,
        R.string.icon_web to Icons.Default.Language,
        R.string.icon_wifi to Icons.Default.Wifi,
        R.string.icon_lock to Icons.Default.Lock,
        R.string.icon_shield to Icons.Default.Shield,
        R.string.icon_apps to Icons.Default.Apps,
    )

    fun getIconByName(name: String?): ImageVector {
        if (name == null) return Icons.Default.Key
        return try {
            val resId = allIcons.keys.find { it.toString() == name } ?: 0
            allIcons[resId] ?: Icons.Default.Key
        } catch (_: Exception) {
            Icons.Default.Key
        }
    }

    fun getIconByRes(resId: Int?): ImageVector {
        return allIcons[resId] ?: Icons.Default.Key
    }

    internal fun getIconByCategory(category: EntryVisualCategory): ImageVector = when (category) {
        EntryVisualCategory.PERSONAL -> Icons.Default.Fingerprint
        EntryVisualCategory.BANK -> Icons.Default.AccountBalance
        EntryVisualCategory.PAYMENT -> Icons.Default.Payments
        EntryVisualCategory.FINANCE -> Icons.AutoMirrored.Filled.TrendingUp
        EntryVisualCategory.ACCOUNT -> Icons.Default.VpnKey
        EntryVisualCategory.SOCIAL -> Icons.Default.Forum
        EntryVisualCategory.EMAIL -> Icons.Default.Email
        EntryVisualCategory.APP -> Icons.Default.Apps
        EntryVisualCategory.GAME -> Icons.Default.SportsEsports
        EntryVisualCategory.VIDEO -> Icons.Default.Subscriptions
        EntryVisualCategory.SHOPPING -> Icons.Default.ShoppingCart
        EntryVisualCategory.HEALTH -> Icons.Default.HealthAndSafety
        EntryVisualCategory.NOTE -> Icons.Default.EditNote
        EntryVisualCategory.WORK -> Icons.Default.Work
        EntryVisualCategory.SCHOOL -> Icons.Default.School
        EntryVisualCategory.TRAVEL -> Icons.Default.Train
        EntryVisualCategory.WIFI -> Icons.Default.Wifi
        EntryVisualCategory.SECURITY -> Icons.Default.Security
        EntryVisualCategory.IDENTITY -> Icons.Default.Badge
        EntryVisualCategory.TECHNICAL -> Icons.Default.Terminal
        EntryVisualCategory.WALLET -> Icons.Default.AccountBalanceWallet
    }
}
