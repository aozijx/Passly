package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.vault.otp.presentation.OtpUiState

@Composable
fun OtpSection(
    type: OtpType,
    currentState: OtpUiState,
    onQrClick: () -> Unit,
    onCodeClick: () -> Unit,
    onPreviousClick: () -> Unit,
) {
    when (type) {
        OtpType.TOTP,
        OtpType.STEAM,
        -> {
            TotpCard(
                currentState = currentState,
                onQrClick = onQrClick,
                onCodeClick = onCodeClick,
            )
        }

        OtpType.HOTP -> {
            HotpCard(currentState, onQrClick, onCodeClick, onPreviousClick)
        }
    }
}

@Composable
internal fun OtpCardHeader(onQrClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.vault_detail_totp_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        IconButton(
            onClick = onQrClick,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                imageVector = Icons.Default.QrCode,
                contentDescription = stringResource(R.string.vault_show_otp_qr),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
