package com.aozijx.passly.presentation.feature.vault.detail.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailOtpUiModel

@Composable
fun TotpSection(
    modifier: Modifier = Modifier,
    currentState: DetailOtpUiModel?,
    qrCode: TotpQrUiState = TotpQrUiState.Hidden,
    showProgress: Boolean = true,
    onQrClick: () -> Unit,
    onQrDismiss: () -> Unit,
    onCodeClick: () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TotpCard(
            currentState = currentState,
            qrCode = qrCode,
            showProgress = showProgress,
            onQrClick = onQrClick,
            onQrDismiss = onQrDismiss,
            onCodeClick = onCodeClick,
        )
    }
}
