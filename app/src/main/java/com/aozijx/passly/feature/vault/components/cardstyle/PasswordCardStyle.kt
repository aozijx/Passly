package com.aozijx.passly.feature.vault.components.cardstyle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aozijx.passly.core.media.ImageResolver.toLocalIconImageModel
import com.aozijx.passly.core.ui.components.VaultItemIcon
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem

private object PasswordCardPaletteCache {
    private const val MAX = 48
    private val cache = object : LinkedHashMap<String, Pair<Color, Color>>(MAX, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Color, Color>>): Boolean {
            return size > MAX
        }
    }

    @Synchronized
    fun get(key: String): Pair<Color, Color>? = cache[key]

    @Synchronized
    fun put(key: String, value: Pair<Color, Color>) {
        cache[key] = value
    }
}

@Composable
fun PasswordStyleVaultItem(
    entry: EntryListItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val secondaryText = when {
        !entry.associatedDomain.isNullOrBlank() -> entry.associatedDomain.orEmpty()
        !entry.associatedAppPackage.isNullOrBlank() -> entry.associatedAppPackage.orEmpty()
        else -> entry.category
    }
    val tertiaryText = when {
        entry.favorite -> "已收藏 · 加密保存"
        !entry.associatedDomain.isNullOrBlank() || !entry.associatedAppPackage.isNullOrBlank() -> "自动填充凭据"
        else -> "受保护的登录凭据"
    }
    val imageModel = remember(entry.iconCustomPath) { toLocalIconImageModel(entry.iconCustomPath) }
    val corner = MaterialTheme.shapes.extraLarge

    var accentColor by remember(imageModel) { mutableStateOf<Color?>(null) }
    var onAccentColor by remember(imageModel) { mutableStateOf<Color?>(null) }

    LaunchedEffect(imageModel) {
        accentColor = null
        onAccentColor = null
        if (imageModel.isNullOrBlank()) return@LaunchedEffect

        PasswordCardPaletteCache.get(imageModel)?.let { cached ->
            accentColor = cached.first
            onAccentColor = cached.second
            return@LaunchedEffect
        }

        runCatching {
            val request =
                ImageRequest.Builder(context).data(imageModel).allowHardware(false).build()
            val result = context.imageLoader.execute(request)
            (result as? SuccessResult)?.drawable?.toBitmap()
        }.getOrNull()?.let { bitmap ->
            val palette = Palette.from(bitmap).clearFilters().generate()
            val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
            swatch?.let {
                val accent = Color(it.rgb)
                val onAccent = Color(it.bodyTextColor)
                accentColor = accent
                onAccentColor = onAccent
                PasswordCardPaletteCache.put(imageModel, accent to onAccent)
            }
        }
    }

    val chipBg = accentColor?.copy(alpha = CardStyleTokens.Password.CHIP_BG_ALPHA)
        ?: MaterialTheme.colorScheme.primaryContainer.copy(alpha = CardStyleTokens.Password.CHIP_FALLBACK_BG_ALPHA)
    val chipFg = onAccentColor ?: MaterialTheme.colorScheme.onPrimaryContainer
    val overlayTop = (accentColor ?: MaterialTheme.colorScheme.primary).copy(
        alpha = if (imageModel.isNullOrBlank()) {
            CardStyleTokens.Password.NO_IMAGE_TOP_OVERLAY_ALPHA
        } else {
            CardStyleTokens.Password.WITH_IMAGE_TOP_OVERLAY_ALPHA
        }
    )
    val overlayBottom = MaterialTheme.colorScheme.surface.copy(
        alpha = if (imageModel.isNullOrBlank()) {
            CardStyleTokens.Password.NO_IMAGE_BOTTOM_OVERLAY_ALPHA
        } else {
            CardStyleTokens.Password.WITH_IMAGE_BOTTOM_OVERLAY_ALPHA
        }
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = CardStyleTokens.Password.maxWidth),
        shape = corner,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = CardStyleTokens.Password.elevation)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(corner)
        ) {
            if (!imageModel.isNullOrBlank()) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(CardStyleTokens.Password.IMAGE_OVERLAY_ALPHA)
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(overlayTop, overlayBottom)
                        )
                    )
            )
            Surface(
                color = Color.Transparent, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(CardStyleTokens.Password.contentPadding)
                ) {
                    VaultItemIcon(Modifier, entry)
                    Spacer(modifier = Modifier.width(CardStyleTokens.Password.iconTextSpacing))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = secondaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tertiaryText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = CardStyleTokens.Password.TERTIARY_TEXT_ALPHA
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(CardStyleTokens.Password.chipCorner),
                        color = chipBg
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                horizontal = CardStyleTokens.Password.chipHorizontalPadding,
                                vertical = CardStyleTokens.Password.chipVerticalPadding
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = chipFg,
                                modifier = Modifier.size(CardStyleTokens.Password.chipIconSize)
                            )
                            Spacer(modifier = Modifier.width(CardStyleTokens.Password.chipIconTextSpacing))
                            Text(
                                text = "PASSWORD",
                                style = MaterialTheme.typography.labelSmall,
                                color = chipFg,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
