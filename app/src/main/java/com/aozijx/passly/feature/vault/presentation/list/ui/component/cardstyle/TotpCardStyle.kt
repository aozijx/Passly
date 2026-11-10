package com.aozijx.passly.feature.vault.presentation.list.ui.component.cardstyle

import com.aozijx.passly.domain.entry.model.EntryType
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultOtpKindUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultOtpUiState
import com.aozijx.passly.feature.vault.presentation.list.model.displayCodeOrPlaceholder
import com.aozijx.passly.feature.vault.presentation.list.model.progress
import com.aozijx.passly.feature.vault.presentation.list.model.shouldDisplayCode
import com.aozijx.passly.presentation.shared.components.VaultItemIcon

@Composable
fun TotpStyleVaultItem(
    entry: VaultListItemUiModel,
    totpState: VaultOtpUiState,
    onClick: () -> Unit,
) {
    val cardShape = MaterialTheme.shapes.extraLarge
    val targetProgress = totpState.progress
    val progress by animateFloatAsState(targetValue = targetProgress, label = "TotpProgress")

    val progressColor by animateColorAsState(
        targetValue =
            when (totpState) {
                VaultOtpUiState.Failed -> MaterialTheme.colorScheme.error
                is VaultOtpUiState.Ready ->
                    if (progress < CardStyleTokens.Totp.LOW_PROGRESS_THRESHOLD) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }

                VaultOtpUiState.NotApplicable,
                VaultOtpUiState.Hidden,
                VaultOtpUiState.NotRequested,
                VaultOtpUiState.Pending,
                -> MaterialTheme.colorScheme.primary
            },
        label = "ProgressColor",
    )

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CardStyleTokens.Totp.elevation),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = CardStyleTokens.Totp.SURFACE_GRADIENT_TOP_ALPHA,
                                    ),
                                    MaterialTheme.colorScheme.surface,
                                ),
                        ),
                    ).padding(CardStyleTokens.Totp.contentPadding),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CardStyleTokens.Totp.rowSpacing),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(CardStyleTokens.Totp.iconContainerSize)
                            .background(
                                color =
                                    MaterialTheme.colorScheme.secondaryContainer.copy(
                                        alpha = CardStyleTokens.Totp.ICON_CONTAINER_ALPHA,
                                    ),
                                shape = RoundedCornerShape(CardStyleTokens.Totp.iconContainerCorner),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    VaultListItemIcon(Modifier, entry)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = entry.categoryLabel.displayText(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(CardStyleTokens.Totp.codeColumnSpacing),
                ) {
                    if (totpState.shouldDisplayCode) {
                        Text(
                            text = totpState.displayCodeOrPlaceholder,
                            style =
                                MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = CardStyleTokens.Totp.codeLetterSpacing,
                                    fontSize = CardStyleTokens.Totp.codeFontSize,
                                ),
                            color = MaterialTheme.colorScheme.primary,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(CardStyleTokens.Totp.progressRowSpacing),
                        ) {
                            CircularWavyProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(CardStyleTokens.Totp.progressSize),
                                color = progressColor,
                                trackColor =
                                    MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = CardStyleTokens.Totp.PROGRESS_TRACK_ALPHA,
                                    ),
                            )
                            Text(
                                text = entry.otpKind.codeTypeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.LockClock,
                            contentDescription = null,
                            tint =
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = CardStyleTokens.Totp.LOCK_ICON_TINT_ALPHA,
                                ),
                            modifier = Modifier.size(CardStyleTokens.Totp.lockIconSize),
                        )
                    }
                }
            }
        }
    }
}

internal object TotpVaultCardStyle : VaultCardStyleComponent {
    override val key: String = "totp"
    override val titleRes: Int = R.string.vault_card_style_totp

    override fun supportsEntryType(entryType: EntryType): Boolean =
        entryType == EntryType.OTP

    override fun supports(entry: VaultListItemUiModel): Boolean = entry.otpKind.isApplicable

    @Composable
    override fun Render(
        entry: VaultListItemUiModel,
        totpState: VaultOtpUiState,
        onClick: () -> Unit,
    ) {
        TotpStyleVaultItem(
            entry = entry,
            totpState = totpState,
            onClick = onClick,
        )
    }

    @Composable
    override fun Preview(entryType: EntryType, onClick: () -> Unit) {
        TotpStyleVaultItem(
            entry = CardStylePreviewFixtures.totpEntry(entryType),
            totpState = CardStylePreviewFixtures.otpState,
            onClick = onClick,
        )
    }
}
