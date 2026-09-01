package com.aozijx.passly.presentation.ui.shared.components

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
import androidx.annotation.StringRes
import com.aozijx.passly.R

internal enum class VaultIconCategory {
    FINANCE,
    IDENTITY,
    COMMUNICATION,
    SECURITY,
    ENTERTAINMENT,
    SHOPPING,
    HEALTH,
    PRODUCTIVITY,
    TRAVEL,
    TECHNOLOGY,
    GENERAL,
}

internal data class VaultIconDefinition(
    val key: String,
    @param:StringRes val labelRes: Int,
    val category: VaultIconCategory,
    val searchAliases: Set<String>,
    val imageVector: ImageVector,
)

object VaultIcons {
    internal val definitions = listOf(
        definition("finance.bank", R.string.icon_bank, VaultIconCategory.FINANCE, Icons.Default.AccountBalance, "bank"),
        definition("finance.card", R.string.icon_card, VaultIconCategory.FINANCE, Icons.Default.CreditCard, "card", "credit"),
        definition("finance.wallet", R.string.icon_wallet, VaultIconCategory.FINANCE, Icons.Default.AccountBalanceWallet, "wallet"),
        definition("finance.bill", R.string.icon_bill, VaultIconCategory.FINANCE, Icons.Default.Payments, "bill", "payment"),
        definition("finance.savings", R.string.icon_savings, VaultIconCategory.FINANCE, Icons.Default.Savings, "savings"),
        definition("finance.trending", R.string.icon_trending, VaultIconCategory.FINANCE, Icons.AutoMirrored.Filled.TrendingUp, "trending", "investment"),
        definition("identity.person", R.string.icon_person, VaultIconCategory.IDENTITY, Icons.Default.Person, "person", "profile"),
        definition("identity.privacy", R.string.icon_privacy, VaultIconCategory.IDENTITY, Icons.Default.Fingerprint, "privacy", "fingerprint"),
        definition("communication.social", R.string.icon_social, VaultIconCategory.COMMUNICATION, Icons.Default.Forum, "social", "forum"),
        definition("communication.groups", R.string.icon_groups, VaultIconCategory.COMMUNICATION, Icons.Default.Groups, "groups", "team"),
        definition("identity.badge", R.string.icon_badge, VaultIconCategory.IDENTITY, Icons.Default.Badge, "badge", "identity"),
        definition("security.key", R.string.icon_key, VaultIconCategory.SECURITY, Icons.Default.VpnKey, "key", "password"),
        definition("communication.email", R.string.icon_email, VaultIconCategory.COMMUNICATION, Icons.Default.Email, "email", "mail"),
        definition("identity.username", R.string.field_username, VaultIconCategory.IDENTITY, Icons.Default.AlternateEmail, "username", "account"),
        definition("entertainment.game", R.string.icon_game, VaultIconCategory.ENTERTAINMENT, Icons.Default.SportsEsports, "game", "gaming"),
        definition("entertainment.video", R.string.icon_video, VaultIconCategory.ENTERTAINMENT, Icons.Default.Subscriptions, "video", "subscription"),
        definition("entertainment.movie", R.string.icon_movie, VaultIconCategory.ENTERTAINMENT, Icons.Default.Movie, "movie", "film"),
        definition("shopping.cart", R.string.icon_shopping, VaultIconCategory.SHOPPING, Icons.Default.ShoppingCart, "shopping", "cart"),
        definition("shopping.bag", R.string.icon_bag, VaultIconCategory.SHOPPING, Icons.Default.ShoppingBag, "bag", "store"),
        definition("entertainment.live", R.string.icon_live, VaultIconCategory.ENTERTAINMENT, Icons.Default.LiveTv, "live", "television"),
        definition("general.camera", R.string.icon_camera, VaultIconCategory.GENERAL, Icons.Default.Videocam, "camera", "video call"),
        definition("general.star", R.string.icon_star, VaultIconCategory.GENERAL, Icons.Default.Star, "star", "favorite"),
        definition("general.heart", R.string.icon_heart, VaultIconCategory.GENERAL, Icons.Default.Favorite, "heart", "favorite"),
        definition("health.safety", R.string.icon_medical, VaultIconCategory.HEALTH, Icons.Default.HealthAndSafety, "medical", "safety"),
        definition("health.services", R.string.icon_health, VaultIconCategory.HEALTH, Icons.Default.MedicalServices, "health", "doctor"),
        definition("technology.cloud", R.string.icon_cloud, VaultIconCategory.TECHNOLOGY, Icons.Default.Cloud, "cloud"),
        definition("productivity.note", R.string.icon_note, VaultIconCategory.PRODUCTIVITY, Icons.Default.EditNote, "note", "edit"),
        definition("productivity.book", R.string.icon_book, VaultIconCategory.PRODUCTIVITY, Icons.Default.Book, "book", "reading"),
        definition("travel.train", R.string.icon_train, VaultIconCategory.TRAVEL, Icons.Default.Train, "train", "rail"),
        definition("travel.flight", R.string.icon_flight, VaultIconCategory.TRAVEL, Icons.Default.Flight, "flight", "airplane"),
        definition("productivity.school", R.string.icon_school, VaultIconCategory.PRODUCTIVITY, Icons.Default.School, "school", "education"),
        definition("productivity.work", R.string.icon_work, VaultIconCategory.PRODUCTIVITY, Icons.Default.Work, "work", "business"),
        definition("technology.code", R.string.icon_code, VaultIconCategory.TECHNOLOGY, Icons.Default.Terminal, "code", "terminal"),
        definition("technology.web", R.string.icon_web, VaultIconCategory.TECHNOLOGY, Icons.Default.Language, "web", "website"),
        definition("technology.wifi", R.string.icon_wifi, VaultIconCategory.TECHNOLOGY, Icons.Default.Wifi, "wifi", "network"),
        definition("security.lock", R.string.icon_lock, VaultIconCategory.SECURITY, Icons.Default.Lock, "lock", "secure"),
        definition("security.shield", R.string.icon_shield, VaultIconCategory.SECURITY, Icons.Default.Shield, "shield", "protection"),
        definition("technology.apps", R.string.icon_apps, VaultIconCategory.TECHNOLOGY, Icons.Default.Apps, "apps", "application"),
    )

    private val definitionsByKey = definitions.associateBy(VaultIconDefinition::key)

    internal fun findDefinition(name: String?): VaultIconDefinition? {
        val value = name?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return definitionsByKey[value]
    }

    internal fun search(
        query: String,
        category: VaultIconCategory? = null,
    ): List<VaultIconDefinition> {
        val normalizedQuery = query.trim()
        return definitions.filter { definition ->
            (category == null || definition.category == category) &&
                (normalizedQuery.isEmpty() || definition.searchAliases.any {
                    it.contains(normalizedQuery, ignoreCase = true)
                })
        }
    }

    fun getIconByName(name: String?): ImageVector {
        return findDefinition(name)?.imageVector ?: Icons.Default.Key
    }

    private fun definition(
        key: String,
        @StringRes labelRes: Int,
        category: VaultIconCategory,
        imageVector: ImageVector,
        vararg aliases: String,
    ) = VaultIconDefinition(
        key = key,
        labelRes = labelRes,
        category = category,
        searchAliases = aliases.toSet(),
        imageVector = imageVector,
    )

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
