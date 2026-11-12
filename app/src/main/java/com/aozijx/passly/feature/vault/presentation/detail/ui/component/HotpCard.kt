package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.otp.presentation.OtpUiState
import com.aozijx.passly.feature.vault.otp.presentation.canRollbackPrevious
import com.aozijx.passly.feature.vault.otp.presentation.displayCodeOrPlaceholder

@Composable
internal fun HotpCard(
    currentState: OtpUiState,
    onQrClick: () -> Unit,
    onCodeClick: () -> Unit,
    onPreviousClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OtpCardHeader(onQrClick)
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                ),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onCodeClick,
                    enabled = currentState is OtpUiState.HotpReady,
                ) {
                    Text(
                        text = currentState.displayCodeOrPlaceholder,
                        style =
                            MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 3.sp,
                                fontFamily = FontFamily.Monospace,
                            ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                TextButton(
                    onClick = onPreviousClick,
                    enabled = currentState.canRollbackPrevious,
                ) {
                    Text(stringResource(R.string.vault_hotp_previous_code))
                }
            }
        }
    }
}
