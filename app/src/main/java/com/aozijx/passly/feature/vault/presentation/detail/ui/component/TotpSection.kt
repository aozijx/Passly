package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailOtpUiModel

@Composable
fun TotpSection(
    modifier: Modifier = Modifier,
    currentState: DetailOtpUiModel?,
    showProgress: Boolean = true,
    onQrClick: () -> Unit,
    onCodeClick: () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TotpCard(
            currentState = currentState,
            showProgress = showProgress,
            onQrClick = onQrClick,
            onCodeClick = onCodeClick,
        )
    }
}
