package com.example.poop.core.designsystem.components

import android.widget.Toast
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.poop.R
import com.example.poop.core.crypto.CryptoManager
import com.example.poop.core.util.ImageType
import com.example.poop.core.util.rememberImagePicker
import com.example.poop.data.model.VaultEntry
import com.example.poop.features.vault.ScannerViewModel
import com.example.poop.features.vault.VaultViewModel
import com.example.poop.util.Logcat
import java.net.URLDecoder

/**
 * Vault 专用的扫码特化组件
 */
@Composable
fun VaultScanner(
    vaultViewModel: VaultViewModel,
    scannerViewModel: ScannerViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // 每次进入扫码页面或退出时，清除之前的扫码结果，防止隐私泄露
    DisposableEffect(Unit) {
        onDispose {
            scannerViewModel.clearScanResult()
        }
    }

    val errorNotOtp = stringResource(R.string.vault_scanner_error_not_otp)
    val successSaveMsg = stringResource(R.string.vault_scanner_success_save)
    val scanResult by scannerViewModel.scanResult.collectAsState()
    
    val scannedTotp = remember(scanResult) { parseOtpAuthUri(scanResult) }

    LaunchedEffect(scanResult) {
        if (scanResult.isNotEmpty() && scannedTotp == null) {
            if (!scanResult.startsWith("otpauth://")) {
                Toast.makeText(context, errorNotOtp, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val pickPhoto = rememberImagePicker { uri, _ ->
        scannerViewModel.decodeImage(context, uri)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        ScannerView(
            scanResult = scanResult,
            showResultCard = scannedTotp == null, 
            onBarcodeDetected = { barcode ->
                if (scannedTotp != null) return@ScannerView
                scannerViewModel.onBarcodeDetected(context, barcode)
            },
            onPermissionDenied = { onDismiss() }
        )

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
                    scannerViewModel.onBarcodeDetected(context, "")
                    pickPhoto(ImageType.SCREEN)
                },
                modifier = Modifier.size(56.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = stringResource(R.string.vault_scanner_action_album), tint = Color.White)
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(56.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close), tint = Color.White)
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
                            text = stringResource(R.string.vault_scanner_result_account, totp.label),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { scannerViewModel.onBarcodeDetected(context, "") }, 
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors()
                            ) {
                                Text(stringResource(R.string.vault_scanner_action_rescan))
                            }
                            
                            Button(
                                onClick = {
                                    val isSteam = (totp.issuer?.contains("Steam", ignoreCase = true) == true) || 
                                                 (totp.label.contains("Steam", ignoreCase = true))
                                    
                                    try {
                                        // 核心修复：直接使用简化后的加密逻辑
                                        val encryptedSecret = CryptoManager.encrypt(totp.secret)
                                        val entry = VaultEntry(
                                            title = totp.issuer ?: totp.label.split(":").firstOrNull() ?: "2FA",
                                            username = totp.label,
                                            password = "",
                                            category = "OTP",
                                            totpSecret = encryptedSecret,
                                            totpDigits = if (isSteam) 5 else (totp.digits ?: 6),
                                            totpAlgorithm = if (isSteam) "STEAM" else (totp.algorithm ?: "SHA1"),
                                            entryType = 1
                                        )
                                        vaultViewModel.addItem(entry)
                                        Toast.makeText(context, successSaveMsg, Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } catch (e: Exception) {
                                        Logcat.e("VaultScanner", "Failed to encrypt/save TOTP", e)
                                        Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.action_save))
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class OtpAuthData(
    val label: String,
    val secret: String,
    val issuer: String?,
    val digits: Int? = null,
    val algorithm: String? = null
)

private fun parseOtpAuthUri(uriString: String): OtpAuthData? {
    if (uriString.isBlank() || !uriString.startsWith("otpauth://")) return null
    return try {
        val uri = uriString.toUri()
        if (uri.host != "totp") return null
        val label = URLDecoder.decode(uri.path?.trimStart('/') ?: "", "UTF-8")
        val secret = uri.getQueryParameter("secret") ?: return null
        val issuer = uri.getQueryParameter("issuer")
        val digits = uri.getQueryParameter("digits")?.toIntOrNull()
        val algorithm = uri.getQueryParameter("algorithm")?.uppercase()
        OtpAuthData(label, secret, issuer, digits, algorithm)
    } catch (_: Exception) {
        null
    }
}
