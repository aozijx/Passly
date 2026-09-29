package com.aozijx.passly.presentation.feature.scanner.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.adaptive.LocalPasslyAdaptiveLayout
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.presentation.feature.scanner.ScannerCameraHost
import com.aozijx.passly.presentation.feature.scanner.ScannerUiAction
import com.aozijx.passly.presentation.feature.scanner.ScannerUiState

@Composable
internal fun ScannerScreen(
    state: ScannerUiState,
    onAction: (ScannerUiAction) -> Unit,
    onPickPhoto: () -> Unit,
    onSaveOtp: (OtpConfig) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val adaptiveLayout = LocalPasslyAdaptiveLayout.current
    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        ScannerCameraHost(
            scanResult = state.scanResult,
            onCopyResult = { onAction(ScannerUiAction.CopyResult) },
            isScanning = state.isScanning,
            showResultCard = state.scannedOtp == null,
            onBarcodeDetected = { barcode ->
                if (state.scannedOtp == null) {
                    onAction(ScannerUiAction.BarcodeDetected(barcode))
                }
            },
            onPermissionDenied = onDismiss,
        )

        ScannerToolbar(
            onPickPhoto = {
                onAction(ScannerUiAction.StartScanning)
                onPickPhoto()
            },
            onDismiss = onDismiss,
        )

        AnimatedVisibility(
            visible = state.scannedOtp != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutHorizontally { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = if (adaptiveLayout.isAtLeastMedium) 24.dp else 12.dp)
                .padding(bottom = if (adaptiveLayout.isAtLeastMedium) 20.dp else 12.dp),
        ) {
            state.scannedOtp?.let { otp ->
                OtpScanResultCard(
                    otp = otp,
                    compact = !adaptiveLayout.isAtLeastMedium,
                    onScanAgain = { onAction(ScannerUiAction.StartScanning) },
                    onSave = {
                        onSaveOtp(otp)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun ScannerToolbar(
    onPickPhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScannerToolbarButton(
            icon = Icons.Default.PhotoLibrary,
            contentDescription = stringResource(R.string.vault_scanner_action_album),
            onClick = onPickPhoto,
        )
        ScannerToolbarButton(
            icon = Icons.Default.Close,
            contentDescription = stringResource(R.string.close),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun ScannerToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White)
    }
}

@Composable
private fun OtpScanResultCard(
    otp: OtpConfig,
    compact: Boolean,
    onScanAgain: () -> Unit,
    onSave: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compact) 12.dp else 20.dp,
                vertical = if (compact) 12.dp else 16.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.vault_scanner_result_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = otp.displayLabel(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onScanAgain,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.filledTonalButtonColors(),
                ) {
                    Text(stringResource(R.string.vault_scan))
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(2f),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

private fun OtpConfig.displayLabel(): String = buildString {
    if (!issuer.isNullOrBlank()) append(issuer)
    if (!accountName.isNullOrBlank()) {
        if (isNotEmpty()) append(": ")
        append(accountName)
    }
    if (isEmpty()) append("TOTP")
}
