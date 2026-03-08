package com.example.poop.ui.screens.vault.components.layout

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.components.ScannerView
import com.example.poop.ui.screens.profile.ImageType
import com.example.poop.ui.screens.scanner.ScannerViewModel
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.utils.CryptoManager
import com.example.poop.util.rememberImagePicker
import java.net.URLDecoder

/**
 * Vault 专用的扫码特化组件
 * 复用 ScannerViewModel 进行图片识别，减少冗余代码
 */
@Composable
fun VaultScanner(
    activity: FragmentActivity,
    vaultViewModel: VaultViewModel,
    scannerViewModel: ScannerViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scanResult by scannerViewModel.scanResult.collectAsState()
    
    // 解析扫码结果是否为有效的 OTP 协议
    val scannedTotp = remember(scanResult) { parseOtpAuthUri(scanResult) }

    // 监听非 OTP 协议的识别结果，给予用户反馈
    LaunchedEffect(scanResult) {
        if (scanResult.isNotEmpty() && scannedTotp == null) {
            // 如果扫到了东西但不是 OTP 协议，给予提示
            if (!scanResult.startsWith("otpauth://")) {
                Toast.makeText(context, "识别成功，但不是有效的 2FA 二维码", Toast.LENGTH_SHORT).show()
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

        // 顶部操作栏
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
                    scannerViewModel.onBarcodeDetected(context, "") // 先重置上次结果
                    pickPhoto(ImageType.SCREEN)
                },
                modifier = Modifier.size(56.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "相册导入", tint = Color.White)
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(56.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
            }
        }

        // 扫码结果确认卡片 (仅针对 OTP 协议)
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
                            text = "发现 2FA 密钥",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "账户: ${totp.label}",
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
                                Text("重扫")
                            }
                            
                            Button(
                                onClick = {
                                    val isSteam = (totp.issuer?.contains("Steam", ignoreCase = true) == true) || 
                                                 (totp.label.contains("Steam", ignoreCase = true))
                                    
                                    // 使用免验证密钥加密，以便后续静默刷新
                                    val cipher = CryptoManager.getEncryptCipher(isSilent = true)
                                    if (cipher != null) {
                                        val encryptedSecret = CryptoManager.encrypt(totp.secret, cipher)
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
                                        Toast.makeText(context, "已成功保存到保险库", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        // 备选方案：如果免验证密钥不可用，则回退到带生物识别验证的加密（确保安全性）
                                        vaultViewModel.encryptMultiple(
                                            activity = activity,
                                            texts = listOf(totp.secret),
                                            title = "保存令牌",
                                            subtitle = "验证身份以保存 ${totp.issuer ?: totp.label}",
                                            onSuccess = { encryptedResults ->
                                                val entry = VaultEntry(
                                                    title = totp.issuer ?: totp.label.split(":").firstOrNull() ?: "2FA",
                                                    username = totp.label,
                                                    password = "",
                                                    category = "OTP",
                                                    totpSecret = encryptedResults[0],
                                                    totpDigits = if (isSteam) 5 else (totp.digits ?: 6),
                                                    totpAlgorithm = if (isSteam) "STEAM" else (totp.algorithm ?: "SHA1"),
                                                    entryType = 1
                                                )
                                                vaultViewModel.addItem(entry)
                                                Toast.makeText(context, "已成功保存到保险库", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.weight(2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("保存到保险库")
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
