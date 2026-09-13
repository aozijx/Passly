package com.aozijx.passly.presentation.feature.vault.detail.component

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.app.qr.QrCodeEncoder
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.ui.vault.detail.component.TotpSection
import com.aozijx.passly.presentation.ui.vault.detail.component.TotpQrUiState
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailOtpUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun DetailOtpHost(
    otp: DetailOtpUiModel?,
    otpQrUri: String?,
    onAction: (DetailUiAction) -> Unit,
    onCopySensitive: (String) -> Unit,
    onOtpQrDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val copySuccessMessage = stringResource(
        R.string.field_copy_success_message,
        stringResource(R.string.vault_detail_totp_label),
    )
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
        onCodeClick = {
            otp?.code?.takeIf { it.isNotEmpty() && !it.contains("-") }?.let { code ->
                onCopySensitive(code)
                Toast.makeText(context, copySuccessMessage, Toast.LENGTH_SHORT).show()
                onAction(DetailUiAction.RecordAction("totp", ActivityType.COPY_PASSWORD))
            }
        },
    )
}
