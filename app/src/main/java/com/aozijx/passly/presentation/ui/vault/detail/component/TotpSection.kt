package com.aozijx.passly.presentation.ui.vault.detail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailOtpUiModel

@Composable
fun TotpSection(
    modifier: Modifier = Modifier,
    currentState: DetailOtpUiModel?,
    totpUri: String? = null,
    showProgress: Boolean = true,
    onQrClick: () -> Unit,
    onQrDismiss: () -> Unit,
    onCodeClick: () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TotpCard(
            currentState = currentState,
            totpUri = totpUri,
            showProgress = showProgress,
            onQrClick = onQrClick,
            onQrDismiss = onQrDismiss,
            onCodeClick = onCodeClick,
        )
    }
}
