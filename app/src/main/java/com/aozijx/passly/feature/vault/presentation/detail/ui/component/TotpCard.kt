package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aozijx.passly.feature.vault.otp.presentation.ui.rememberOtpWindowProgress
import com.aozijx.passly.feature.vault.otp.presentation.OtpUiState
import com.aozijx.passly.feature.vault.otp.presentation.displayCodeOrPlaceholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotpCard(
    currentState: OtpUiState,
    showProgress: Boolean = true,
    onQrClick: () -> Unit,
    onCodeClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OtpCardHeader(onQrClick)

        Card(
            onClick = onCodeClick,
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.primaryContainer.copy(
                            alpha = 0.2f,
                        ),
                ),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        ) {
            val progressState =
                rememberOtpWindowProgress(
                    state = currentState,
                )

            val codeStyle =
                MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace,
                )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showProgress) {
                    TotpCircularWavyProgressIndicator(
                        progress = { progressState.progress.value },
                        isExpiring = progressState.isExpiring.value,
                        modifier = Modifier.size(42.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = currentState.displayCodeOrPlaceholder,
                    style = codeStyle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.wrapContentWidth(),
                )
            }
        }
    }
}

@Composable
private fun TotpCircularWavyProgressIndicator(
    progress: () -> Float,
    isExpiring: Boolean,
    modifier: Modifier = Modifier,
) {
    val progressColor =
        if (isExpiring) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }
    CircularWavyProgressIndicator(
        progress = { progress().coerceIn(0f, 1f) },
        modifier = modifier,
        color = progressColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}
