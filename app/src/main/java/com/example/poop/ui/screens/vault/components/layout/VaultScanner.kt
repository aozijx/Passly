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
import com.example.poop.ui.screens.components.ScannerView
import com.example.poop.ui.screens.profile.ImageType
import com.example.poop.ui.screens.scanner.ScannerViewModel
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.utils.VaultSecurityUtils
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

    // 解析扫码结果
    val scannedTotp = remember(scanResult) { parseOtpAuthUri(scanResult) }

    // 直接复用通用的图片选取器，并调用 scannerViewModel 的识别逻辑
    val pickPhoto = rememberImagePicker { uri, _ ->
        scannerViewModel.decodeImage(context, uri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        ScannerView(
            onBarcodeDetected = { barcode ->
                if (scannedTotp != null) return@ScannerView
                scannerViewModel.onBarcodeDetected(context, barcode)
            },
            onPermissionDenied = { onDismiss() }
        )

        // 扫码结果确认卡片
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
                            text = "发现 TOTP 密钥",
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
                                onClick = {
                                    scannerViewModel.onBarcodeDetected(
                                        context,
                                        ""
                                    )
                                }, // 清空结果以重新扫描
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors()
                            ) {
                                Text("重扫")
                            }

                            Button(
                                onClick = {
                                    VaultSecurityUtils.encryptMultiple(
                                        activity = activity,
                                        texts = listOf(totp.secret),
                                        title = "保存令牌",
                                        subtitle = "验证身份以保存 ${totp.issuer ?: totp.label}",
                                        onSuccess = { encryptedResults: List<String> ->
                                            vaultViewModel.addItem(
                                                title = totp.issuer ?: totp.label.split(":")
                                                    .firstOrNull() ?: "TOTP",
                                                encryptedUser = totp.label,
                                                encryptedPass = "",
                                                category = "OTP",
                                                totpSecret = encryptedResults[0]
                                            )
                                            Toast.makeText(
                                                context,
                                                "已成功保存到保险库",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onDismiss()
                                        }
                                    )
                                },
                                modifier = Modifier.weight(2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("保存到保险库")
                            }
                        }
                    }
                }
            }
        }
        // 底部操作栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { pickPhoto(ImageType.SCREEN) },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "相册导入", tint = Color.White)
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
            }
        }
    }
}

private data class OtpAuthData(
    val label: String,
    val secret: String,
    val issuer: String?
)

private fun parseOtpAuthUri(uriString: String): OtpAuthData? {
    if (uriString.isBlank()) return null
    return try {
        val uri = uriString.toUri()
        if (uri.scheme != "otpauth" || uri.host != "totp") return null
        val label = URLDecoder.decode(uri.path?.trimStart('/') ?: "", "UTF-8")
        val secret = uri.getQueryParameter("secret") ?: return null
        val issuer = uri.getQueryParameter("issuer")
        OtpAuthData(label, secret, issuer)
    } catch (_: Exception) {
        null
    }
}
