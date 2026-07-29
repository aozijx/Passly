package com.aozijx.passly.feature.vault.components.cardstyle

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.aozijx.passly.core.ui.components.VaultItemIcon
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.vault.model.OtpUiState

private object TotpBehaviorTokens {
    const val FALLBACK_CODE = "------"
    const val STEAM_LABEL = "STEAM"
    const val TOTP_LABEL = "TOTP"

    const val LOW_PROGRESS_THRESHOLD = 0.2f
}

@Composable
fun TotpStyleVaultItem(
    entry: EntryListItem,
    totpState: OtpUiState?,
    showCode: Boolean = true,
    previewCode: String? = null,
    previewProgress: Float? = null,
    onClick: () -> Unit
) {
    val cardShape = MaterialTheme.shapes.extraLarge
    val currentState =
        previewCode?.let { OtpUiState(code = it, progress = previewProgress ?: 0f) } ?: totpState
    val isSteam = remember(entry.otpType) {
        entry.otpType == OtpType.STEAM
    }

    val targetProgress = previewProgress ?: (currentState?.progress ?: 0f)
    val progress by animateFloatAsState(targetValue = targetProgress, label = "TotpProgress")

    val shownCode = previewCode ?: run {
        val rawCode = currentState?.code
        if (rawCode == null) TotpBehaviorTokens.FALLBACK_CODE
        else if (isSteam) rawCode
        else {
            when (rawCode.length) {
                6 -> "${rawCode.take(3)} ${rawCode.drop(3)}"
                8 -> "${rawCode.take(4)} ${rawCode.drop(4)}"
                else -> rawCode.chunked(3).joinToString(" ")
            }
        }
    }

    val progressColor by animateColorAsState(
        targetValue = if (progress < TotpBehaviorTokens.LOW_PROGRESS_THRESHOLD) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary, label = "ProgressColor"
    )

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CardStyleTokens.Totp.elevation)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = CardStyleTokens.Totp.SURFACE_GRADIENT_TOP_ALPHA
                            ), MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(CardStyleTokens.Totp.contentPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CardStyleTokens.Totp.rowSpacing)
            ) {
                Box(
                    modifier = Modifier
                        .size(CardStyleTokens.Totp.iconContainerSize)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(
                                alpha = CardStyleTokens.Totp.ICON_CONTAINER_ALPHA
                            ),
                            shape = RoundedCornerShape(CardStyleTokens.Totp.iconContainerCorner)
                        ), contentAlignment = Alignment.Center
                ) {
                    VaultItemIcon(Modifier, entry)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entry.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(CardStyleTokens.Totp.codeColumnSpacing)
                ) {
                    if (showCode) {
                        Text(
                            text = shownCode, style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = CardStyleTokens.Totp.codeLetterSpacing,
                                fontSize = CardStyleTokens.Totp.codeFontSize
                            ), color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(CardStyleTokens.Totp.progressRowSpacing)
                        ) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(CardStyleTokens.Totp.progressSize),
                                strokeWidth = CardStyleTokens.Totp.progressStrokeWidth,
                                color = progressColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = CardStyleTokens.Totp.PROGRESS_TRACK_ALPHA
                                )
                            )
                            Text(
                                text = if (isSteam) TotpBehaviorTokens.STEAM_LABEL else TotpBehaviorTokens.TOTP_LABEL,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.LockClock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(
                                alpha = CardStyleTokens.Totp.LOCK_ICON_TINT_ALPHA
                            ),
                            modifier = Modifier.size(CardStyleTokens.Totp.lockIconSize)
                        )
                    }
                }
            }
        }
    }
}
