package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailOtpUiModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotpCard(
    currentState: DetailOtpUiModel?,
    showProgress: Boolean = true,
    onQrClick: () -> Unit,
    onCodeClick: (() -> Unit)? = null
) {

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.vault_detail_totp_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            IconButton(
                onClick = {
                    onQrClick()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.QrCode, contentDescription = stringResource(R.string.vault_show_otp_qr),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            onClick = { onCodeClick?.invoke() },
            modifier = Modifier.fillMaxWidth(),
            enabled = onCodeClick != null,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                    alpha = 0.2f
                )
            ),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            val readyState = currentState as? DetailOtpUiModel.Ready
            val code = readyState?.code
            val progress = readyState?.progress ?: 0f

            val displayText = remember(code) {
                code?.chunked(3)?.joinToString(" ") ?: "------"
            }

            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                label = "otp_progress"
            )

            val codeStyle = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showProgress) {
                    TotpCircularWavyProgressIndicator(
                        progress = animatedProgress,
                        isExpiring = progress < 0.2f,
                        modifier = Modifier.size(42.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = displayText,
                    style = codeStyle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.wrapContentWidth()
                )
            }
        }
    }
}

@Composable
private fun TotpCircularWavyProgressIndicator(
    progress: Float,
    isExpiring: Boolean,
    modifier: Modifier = Modifier
) {
    val progressColor = if (isExpiring) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    CircularWavyProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier,
        color = progressColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
