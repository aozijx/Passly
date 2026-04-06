package com.aozijx.passly.features.vault.components.entries

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.aozijx.passly.core.designsystem.icons.VaultItemIcon
import com.aozijx.passly.core.designsystem.model.VaultCardStyleTokens
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.features.vault.VaultViewModel

private object TotpBehaviorTokens {
    const val FALLBACK_CODE = "------"
    const val STEAM_LABEL = "STEAM"
    const val TOTP_LABEL = "TOTP"

    const val LOW_PROGRESS_THRESHOLD = 0.2f
}

@Composable
fun TotpStyleVaultItem(
    entry: VaultSummary,
    vaultViewModel: VaultViewModel? = null,
    showCode: Boolean = true,
    previewCode: String? = null,
    previewProgress: Float? = null,
    onClick: () -> Unit = { vaultViewModel?.showDetail(entry) }
) {
    val totpStatesState = vaultViewModel?.totpStates?.collectAsState()
    val currentState = totpStatesState?.value?.get(entry.id)
    val isSteam = remember(entry.totpAlgorithm) { entry.totpAlgorithm.uppercase() == "STEAM" }

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

    LaunchedEffect(entry.id, vaultViewModel) {
        vaultViewModel?.autoUnlockTotp(entry)
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = VaultCardStyleTokens.Totp.marginHorizontal,
                vertical = VaultCardStyleTokens.Totp.marginVertical
            ),
        shape = RoundedCornerShape(VaultCardStyleTokens.Totp.corner),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = VaultCardStyleTokens.Totp.elevation)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(VaultCardStyleTokens.Totp.corner))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = VaultCardStyleTokens.Totp.SURFACE_GRADIENT_TOP_ALPHA
                            ), MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(VaultCardStyleTokens.Totp.contentPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VaultCardStyleTokens.Totp.rowSpacing)
            ) {
                // 图标区域
                Box(
                    modifier = Modifier
                        .size(VaultCardStyleTokens.Totp.iconContainerSize)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(
                                alpha = VaultCardStyleTokens.Totp.ICON_CONTAINER_ALPHA
                            ),
                            shape = RoundedCornerShape(VaultCardStyleTokens.Totp.iconContainerCorner)
                        ), contentAlignment = Alignment.Center
                ) {
                    VaultItemIcon(item = entry)
                }

                // 信息区域
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

                // 代码与进度区域
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(VaultCardStyleTokens.Totp.codeColumnSpacing)
                ) {
                    if (showCode) {
                        Text(
                            text = shownCode, style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = VaultCardStyleTokens.Totp.codeLetterSpacing,
                                fontSize = VaultCardStyleTokens.Totp.codeFontSize
                            ), color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(VaultCardStyleTokens.Totp.progressRowSpacing)
                        ) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(VaultCardStyleTokens.Totp.progressSize),
                                strokeWidth = VaultCardStyleTokens.Totp.progressStrokeWidth,
                                color = progressColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = VaultCardStyleTokens.Totp.PROGRESS_TRACK_ALPHA
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
                                alpha = VaultCardStyleTokens.Totp.LOCK_ICON_TINT_ALPHA
                            ),
                            modifier = Modifier.size(VaultCardStyleTokens.Totp.lockIconSize)
                        )
                    }
                }
            }
        }
    }
}
