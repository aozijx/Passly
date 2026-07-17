package com.aozijx.passly.feature.vault.components.cardstyle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.ui.components.VaultItemIcon

@Composable
fun VaultItem(
    entry: VaultEntry,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardStyleTokens.Base.corner),
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
                    text = entry.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TwoFAItem(
    entry: VaultEntry,
    vaultViewModel: VaultViewModel?,
    showCode: Boolean = true,
    previewCode: String? = null,
    previewProgress: Float? = null,
    onClick: () -> Unit = { vaultViewModel?.showDetail(entry) }
) {
    val currentState =
        vaultViewModel?.uiState?.collectAsStateWithLifecycle()?.value?.totpStates?.get(entry.id)

    val isSteam = remember(entry.credential.twoFactor?.otp?.algorithm ?: "SHA1") { (entry.credential.twoFactor?.otp?.algorithm ?: "SHA1").uppercase() == "STEAM" }

    LaunchedEffect(entry.id, vaultViewModel) {
        vaultViewModel?.autoUnlockTotp(entry)
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                    text = entry.category,
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

@Composable
fun AutoFillItem(
    entry: VaultEntry,
    viewModel: VaultViewModel,
    onClick: () -> Unit = { viewModel.showDetail(entry) }
) {
    val isAutoCaptured = entry.category == stringResource(R.string.category_autofill)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center
            ) {
                VaultItemIcon(
                    modifier = Modifier.size(36.dp), entry
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isAutoCaptured) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Security,
                            contentDescription = stringResource(R.string.autofill_pending_verification),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = entry.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}