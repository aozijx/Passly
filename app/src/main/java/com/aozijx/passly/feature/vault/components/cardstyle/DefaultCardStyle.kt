package com.aozijx.passly.feature.vault.components.cardstyle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aozijx.passly.core.ui.components.VaultItemIcon
import com.aozijx.passly.feature.vault.model.OtpUiState
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.otp.OtpType

@Composable
fun VaultItem(
    entry: EntryListItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = CardStyleTokens.Base.CONTAINER_ALPHA
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(CardStyleTokens.Base.contentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VaultItemIcon(
                Modifier,
                entry,
            )

            Spacer(modifier = Modifier.width(CardStyleTokens.Base.iconTextSpacing))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.categoryOrTemplateLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TwoFAItem(
    entry: EntryListItem,
    totpState: OtpUiState?,
    showCode: Boolean = true,
    previewCode: String? = null,
    previewProgress: Float? = null,
    onClick: () -> Unit
) {
    val currentState =
        previewCode?.let { OtpUiState(code = it, progress = previewProgress ?: 0f) } ?: totpState

    val isSteam = remember(entry.otpType) {
        entry.otpType == OtpType.STEAM
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            VaultItemIcon(
                Modifier, entry
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.categoryOrTemplateLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            CircularProgressIndicator(
                progress = { previewProgress ?: currentState?.progress ?: 0f },
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp,
                color = if ((previewProgress ?: currentState?.progress ?: 1f) < 0.2f) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )

            if (showCode) {
                Text(
                    text = if (isSteam) {
                        previewCode ?: currentState?.code ?: "------"
                    } else {
                        previewCode ?: currentState?.code?.chunked(3)?.joinToString(" ") ?: "------"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

internal object DefaultVaultCardStyle : VaultCardStyleComponent {
    override val key: String = "default"

    @Composable
    override fun Render(
        entry: EntryListItem,
        totpState: OtpUiState?,
        showTotpCode: Boolean,
        onClick: () -> Unit,
    ) {
        when {
            entry.hasOtp -> TwoFAItem(
                entry = entry,
                totpState = totpState,
                showCode = showTotpCode,
                onClick = onClick,
            )

            else -> VaultItem(entry = entry, onClick = onClick)
        }
    }

    @Composable
    override fun Preview(onClick: () -> Unit) {
        VaultItem(
            entry = CardStylePreviewFixtures.defaultEntry,
            onClick = onClick,
        )
    }
}
