package com.aozijx.passly.feature.vault.presentation.list.ui.component.cardstyle

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.otp.presentation.OtpPresentation
import com.aozijx.passly.feature.vault.otp.presentation.OtpUiState
import com.aozijx.passly.feature.vault.otp.presentation.displayCodeOrPlaceholder
import com.aozijx.passly.feature.vault.otp.presentation.shouldDisplayCode
import com.aozijx.passly.feature.vault.otp.presentation.ui.rememberOtpWindowProgress
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemUiModel
import com.aozijx.passly.feature.vault.presentation.list.preview.CardStylePreviewFixtures
import com.aozijx.passly.presentation.shared.components.VaultItemIcon

@Composable
fun DefaultItem(
    entry: VaultListItemUiModel,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = CardStyleTokens.Base.CONTAINER_ALPHA,
                    ),
            ),
    ) {
        Row(
            modifier = Modifier.padding(CardStyleTokens.Base.contentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VaultListItemIcon(
                Modifier,
                entry,
            )

            Spacer(modifier = Modifier.width(CardStyleTokens.Base.iconTextSpacing))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = entry.categoryLabel.displayText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun DefaultOtpItem(
    entry: VaultListItemUiModel,
    totpState: OtpUiState,
    onClick: () -> Unit,
) {
    val progressState =
        rememberOtpWindowProgress(
            state = totpState,
            expiringThreshold = CardStyleTokens.Totp.LOW_PROGRESS_THRESHOLD,
        )
    val progressColor =
        if (totpState is OtpUiState.TimedReady && progressState.isExpiring.value) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VaultListItemIcon(
                Modifier,
                entry,
            )

            Spacer(modifier = Modifier.width(20.dp))

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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            CircularProgressIndicator(
                progress = { progressState.progress.value },
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp,
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            )

            if (totpState.shouldDisplayCode) {
                Text(
                    text = totpState.displayCodeOrPlaceholder,
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            fontSize = 18.sp,
                        ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp),
                )

                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

internal object DefaultVaultCardStyle : VaultCardStyleComponent {
    override val key: String = "default"
    override val titleRes: Int = R.string.vault_card_style_default

    @Composable
    override fun Render(
        entry: VaultListItemUiModel,
        totpState: OtpUiState,
        onClick: () -> Unit,
    ) {
        when {
            entry.otpPresentation is OtpPresentation.Applicable -> {
                DefaultOtpItem(
                    entry = entry,
                    totpState = totpState,
                    onClick = onClick,
                )
            }

            else -> {
                DefaultItem(entry = entry, onClick = onClick)
            }
        }
    }

    @Composable
    override fun Preview(
        entryType: EntryType,
        onClick: () -> Unit,
    ) {
        val entry = CardStylePreviewFixtures.defaultEntry(entryType)
        if (entry.otpPresentation is OtpPresentation.Applicable) {
            DefaultOtpItem(
                entry = entry,
                totpState = CardStylePreviewFixtures.otpState,
                onClick = onClick,
            )
        } else {
            DefaultItem(entry = entry, onClick = onClick)
        }
    }
}
