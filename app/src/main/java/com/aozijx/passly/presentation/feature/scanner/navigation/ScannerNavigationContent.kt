package com.aozijx.passly.presentation.feature.scanner.navigation

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.presentation.feature.scanner.VaultScanner

@Composable
internal fun VaultOtpScannerContent(
    onResult: (OtpConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    VaultScanner(onSaveOtp = onResult, onDismiss = onDismiss)
}
