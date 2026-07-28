package com.aozijx.passly.feature.scanner

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.media.ImageType
import com.aozijx.passly.core.media.rememberImagePicker
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.feature.scanner.components.ScannerView
import com.aozijx.passly.feature.scanner.contract.ScannerEffect
import com.aozijx.passly.feature.scanner.contract.ScannerIntent

/**
 * Vault 专用的扫码特化组件
 */
@Composable
fun VaultScanner(
    onSaveOtp: (OtpConfig) -> Unit,
    scannerViewModel: ScannerViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scannerUiState by scannerViewModel.uiState.collectAsStateWithLifecycle()
    // 适配系统返回手势：优先关闭扫码层，而不是直接退出上层页面。
    BackHandler(onBack = onDismiss)

    val errorNotOtp = stringResource(R.string.vault_scanner_error_not_otp)
    // 使用 Effect 接收一次性扫描结果
    var scanResult by remember { mutableStateOf("") }
    var scannedTotp by remember { mutableStateOf<OtpConfig?>(null) }

    LaunchedEffect(scannerViewModel) {
        scannerViewModel.handleIntent(ScannerIntent.StartScanning)
        scannerViewModel.effects.collect { effect ->
            when (effect) {
                is ScannerEffect.ScanSuccess -> {
                    scanResult = effect.result
                    scannedTotp = effect.otpConfig
                    if (scannedTotp == null && !effect.result.startsWith("otpauth://")) {
                        Toast.makeText(context, errorNotOtp, Toast.LENGTH_SHORT).show()
                    }
                }

                is ScannerEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val pickPhoto = rememberImagePicker { uri, _ ->
        scannerViewModel.handleIntent(ScannerIntent.DecodeImage(uri))
    }

    DisposableEffect(scannerViewModel) {
        onDispose {
            scannerViewModel.handleIntent(ScannerIntent.StopScanning)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        ScannerView(
            scanResult = scanResult,
            isScanning = scannerUiState.isScanning,
            showResultCard = scannedTotp == null,
            onBarcodeDetected = { barcode ->
                if (scannedTotp != null) return@ScannerView
                scannerViewModel.handleIntent(ScannerIntent.BarcodeDetected(barcode))
            },
            onPermissionDenied = { onDismiss() })

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    scanResult = ""
                    scannedTotp = null
                    scannerViewModel.handleIntent(ScannerIntent.StartScanning)
                    pickPhoto(ImageType.SCREEN)
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = stringResource(R.string.vault_scanner_action_album),
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White
                )
            }
        }

        AnimatedVisibility(
            visible = scannedTotp != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutHorizontally { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            scannedTotp?.let { totp ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.vault_scanner_result_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = buildString {
                                if (!totp.issuer.isNullOrBlank()) append(totp.issuer)
                                if (!totp.accountName.isNullOrBlank()) {
                                    if (isNotEmpty()) append(": ")
                                    append(totp.accountName)
                                }
                                if (isEmpty()) append("TOTP")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scanResult = ""
                                    scannedTotp = null
                                    scannerViewModel.handleIntent(ScannerIntent.StartScanning)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors()
                            ) {
                                Text(stringResource(R.string.vault_scan))
                            }

                            Button(
                                onClick = {
                                    val parsedConfig = scannedTotp ?: return@Button
                                    onSaveOtp(parsedConfig)
                                    onDismiss()
                                }, modifier = Modifier.weight(2f), shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.save))
                            }
                        }
                    }
                }
            }
        }
    }
}
