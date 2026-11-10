package com.aozijx.passly.feature.vault.presentation.list.ui.component.cardstyle

import com.aozijx.passly.domain.entry.model.EntryType
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.aozijx.passly.R
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
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultOtpUiState
import com.aozijx.passly.feature.vault.presentation.list.model.VaultPasswordCardSecondaryTextUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultPasswordCardSupportingTextUiModel
import com.aozijx.passly.presentation.shared.components.VaultItemIcon
import com.aozijx.passly.presentation.shared.entry.labelRes

@Composable
fun PasswordStyleVaultItem(
    entry: VaultListItemUiModel,
    onClick: () -> Unit,
) {
    val secondaryText =
        when (val value = entry.passwordCardSecondaryText) {
            is VaultPasswordCardSecondaryTextUiModel.Value -> value.text
            is VaultPasswordCardSecondaryTextUiModel.EntryTypeLabel -> stringResource(value.entryType.labelRes)
        }
    val tertiaryText =
        stringResource(
            when (entry.passwordCardSupportingText) {
                VaultPasswordCardSupportingTextUiModel.FAVORITE -> R.string.vault_password_card_favorite
                VaultPasswordCardSupportingTextUiModel.AUTOFILL -> R.string.vault_password_card_autofill
                VaultPasswordCardSupportingTextUiModel.PROTECTED -> R.string.vault_password_card_protected
            },
        )
    val imageModel = entry.iconImageModel
    val corner = MaterialTheme.shapes.extraLarge
    val paletteColors = rememberImagePaletteColors(imageModel?.uri)
    val accentColor = paletteColors?.accent
    val overlayTop =
        (accentColor ?: MaterialTheme.colorScheme.primary).copy(
            alpha =
                if (imageModel == null) {
                    CardStyleTokens.Password.NO_IMAGE_TOP_OVERLAY_ALPHA
                } else {
                    CardStyleTokens.Password.WITH_IMAGE_TOP_OVERLAY_ALPHA
                },
        )
    val overlayBottom =
        MaterialTheme.colorScheme.surface.copy(
            alpha =
                if (imageModel == null) {
                    CardStyleTokens.Password.NO_IMAGE_BOTTOM_OVERLAY_ALPHA
                } else {
                    CardStyleTokens.Password.WITH_IMAGE_BOTTOM_OVERLAY_ALPHA
                },
        )

    Card(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .widthIn(max = CardStyleTokens.Password.maxWidth),
        shape = corner,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = CardStyleTokens.Password.elevation),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(corner),
        ) {
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .matchParentSize()
                            .alpha(CardStyleTokens.Password.IMAGE_OVERLAY_ALPHA),
                )
            }
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(overlayTop, overlayBottom),
                            ),
                        ),
            )
            Surface(
                color = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(CardStyleTokens.Password.contentPadding),
                ) {
                    VaultListItemIcon(Modifier, entry)
                    Spacer(modifier = Modifier.width(CardStyleTokens.Password.iconTextSpacing))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = secondaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = tertiaryText,
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = CardStyleTokens.Password.TERTIARY_TEXT_ALPHA,
                                ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

internal object PasswordVaultCardStyle : VaultCardStyleComponent {
    override val key: String = "password"
    override val titleRes: Int = R.string.vault_card_style_password

    override fun supportsEntryType(entryType: EntryType): Boolean = entryType == EntryType.LOGIN

    override fun supports(entry: VaultListItemUiModel): Boolean = entry.hasPassword

    @Composable
    override fun Render(
        entry: VaultListItemUiModel,
        totpState: VaultOtpUiState,
        onClick: () -> Unit,
    ) {
        PasswordStyleVaultItem(entry = entry, onClick = onClick)
    }

    @Composable
    override fun Preview(entryType: EntryType, onClick: () -> Unit) {
        PasswordStyleVaultItem(
            entry = CardStylePreviewFixtures.passwordEntry(entryType),
            onClick = onClick,
        )
    }
}
