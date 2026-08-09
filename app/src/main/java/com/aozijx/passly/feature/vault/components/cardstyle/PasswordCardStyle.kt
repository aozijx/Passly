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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aozijx.passly.core.media.rememberImagePaletteColors
import com.aozijx.passly.core.media.toLocalIconImageModel
import com.aozijx.passly.core.ui.components.VaultItemIcon
import com.aozijx.passly.domain.entry.model.OtpUiState
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem

@Composable
fun PasswordStyleVaultItem(
    entry: EntryListItem,
    onClick: () -> Unit
) {
    val secondaryText = when {
        !entry.associatedDomain.isNullOrBlank() -> entry.associatedDomain.orEmpty()
        !entry.associatedAppPackage.isNullOrBlank() -> entry.associatedAppPackage.orEmpty()
        else -> entry.categoryOrTemplateLabel()
    }
    val tertiaryText = when {
        entry.favorite -> "已收藏 · 加密保存"
        !entry.associatedDomain.isNullOrBlank() || !entry.associatedAppPackage.isNullOrBlank() -> "自动填充凭据"
        else -> "受保护的登录凭据"
    }
    val imageModel = remember(entry.iconCustomPath) { toLocalIconImageModel(entry.iconCustomPath) }
    val corner = MaterialTheme.shapes.extraLarge
    val paletteColors = rememberImagePaletteColors(imageModel)
    val accentColor = paletteColors?.accent
    val onAccentColor = paletteColors?.onAccent

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

internal object PasswordVaultCardStyle : VaultCardStyleComponent {
    override val key: String = "password"

    override fun supports(entry: EntryListItem): Boolean = entry.hasPassword

    @Composable
    override fun Render(
        entry: EntryListItem,
        totpState: OtpUiState?,
        showTotpCode: Boolean,
        onClick: () -> Unit,
    ) {
        PasswordStyleVaultItem(entry = entry, onClick = onClick)
    }

    @Composable
    override fun Preview(onClick: () -> Unit) {
        PasswordStyleVaultItem(
            entry = CardStylePreviewFixtures.passwordEntry,
            onClick = onClick,
        )
    }
}
