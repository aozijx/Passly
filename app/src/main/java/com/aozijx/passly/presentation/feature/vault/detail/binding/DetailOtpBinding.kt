package com.aozijx.passly.presentation.feature.vault.detail.binding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.aozijx.passly.app.qr.QrCodeEncoder
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.ui.vault.detail.component.TotpSection
import com.aozijx.passly.presentation.ui.vault.detail.component.TotpQrUiState
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailOtpUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun DetailOtpBinding(
    otp: DetailOtpUiModel?,
    otpQrUri: String?,
    onAction: (DetailUiAction) -> Unit,
    onOtpQrDismiss: () -> Unit,
) {
    val qrCode by produceState<TotpQrUiState>(
        initialValue = if (otpQrUri == null) TotpQrUiState.Hidden else TotpQrUiState.Loading,
        key1 = otpQrUri,
    ) {
        value = if (otpQrUri == null) {
            TotpQrUiState.Hidden
        } else {
            value = TotpQrUiState.Loading
            withContext(Dispatchers.Default) {
                QrCodeEncoder.encode(otpQrUri)
            }?.let(TotpQrUiState::Ready) ?: TotpQrUiState.Failed
        }
    }
    TotpSection(
        currentState = otp,
        qrCode = qrCode,
        onQrClick = { onAction(DetailUiAction.ExportOtpQr) },
        onQrDismiss = onOtpQrDismiss,
        onCodeClick = { onAction(DetailUiAction.CopyOtpCode) },
    )
}
