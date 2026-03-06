package com.example.poop.ui.screens.vault.twoFA

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.components.common.CategoryItem
import com.example.poop.ui.screens.vault.components.common.DetailActions
import com.example.poop.ui.screens.vault.components.common.DetailHeader
import com.example.poop.ui.screens.vault.utils.TwoFAUtils
import com.example.poop.ui.screens.vault.utils.VaultSecurityUtils
import com.example.poop.util.ClipboardUtils
import com.example.poop.util.QrCodeUtils
import kotlinx.coroutines.delay
import java.net.URLEncoder

@Composable
fun TwoFADetailDialog(
    activity: FragmentActivity,
    item: VaultEntry,
    viewModel: VaultViewModel
) {
    val context = LocalContext.current
    var decryptedSecret by remember { mutableStateOf<String?>(null) }
    var totpCode by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(1f) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }

    // 1. 内部解密逻辑
    LaunchedEffect(item.id, item.totpSecret) {
        if (item.totpSecret == null) return@LaunchedEffect
        VaultSecurityUtils.decryptSingle(
            activity = activity,
            encryptedText = item.totpSecret,
            onFailure = { viewModel.dismissDetail() }
        ) { decryptedSecret = it }
    }

    // 2. 验证码生成逻辑
    val isSteam = remember(item.totpAlgorithm) { item.totpAlgorithm.uppercase() == "STEAM" }
    LaunchedEffect(decryptedSecret, item.totpAlgorithm, item.totpDigits, item.totpPeriod) {
        val secret = decryptedSecret ?: return@LaunchedEffect
        while (true) {
            val period = item.totpPeriod.coerceAtLeast(1)
            val currentTime = System.currentTimeMillis() / 1000
            val remaining = period - (currentTime % period)
            progress = remaining.toFloat() / period

            totpCode = TwoFAUtils.generateTotp(
                secret = secret,
                digits = if (isSteam) 5 else item.totpDigits,
                period = item.totpPeriod,
                algorithm = item.totpAlgorithm
            )
            delay(500)
        }
    }

    // 只有解密成功后才显示 UI，避免闪烁或空显示
    if (decryptedSecret == null) return

    AlertDialog(
        onDismissRequest = { viewModel.dismissDetail() },
        title = {
            DetailHeader(
                item = item,
                onIconClick = { viewModel.showIconPicker = true },
                onMoreClick = { showAdvancedSettings = !showAdvancedSettings }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                CategoryItem(viewModel = viewModel, entry = item)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("动态验证码", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    IconButton(onClick = { showQrDialog = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.QrCode, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        ClipboardUtils.copy(context, totpCode)
                        Toast.makeText(context, "验证码已复制", Toast.LENGTH_SHORT).show()
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isSteam) totpCode else totpCode.chunked(3).joinToString(" "),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = if (isSteam) 4.sp else 2.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(24.dp))
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 4.dp,
                            color = if (progress < 0.2f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }

                if (showAdvancedSettings) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    AddTwoFASection(activity = activity, item = item, viewModel = viewModel, revealedSecret = decryptedSecret)
                }
            }
        },
        confirmButton = {
            DetailActions(onDeleteClick = { viewModel.requestDelete(item) }, onDismiss = { viewModel.dismissDetail() })
        }
    )

    if (showQrDialog) {
        val qrContent = remember(item, decryptedSecret) { constructOtpAuthUri(item, decryptedSecret!!) }
        val qrBitmap = remember(qrContent) { QrCodeUtils.generateQrCode(qrContent) }
        QrExportDialog(bitmap = qrBitmap, onDismiss = { showQrDialog = false })
    }
}

@Composable
private fun QrExportDialog(bitmap: Bitmap?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出二维码") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("使用其他验证器应用扫描此二维码即可迁移此令牌。", style = MaterialTheme.typography.bodyMedium)
                if (bitmap != null) {
                    Card(modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(1f), shape = RoundedCornerShape(12.dp)) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.padding(16.dp).fillMaxSize())
                        }
                    }
                } else {
                    CircularProgressIndicator()
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

private fun constructOtpAuthUri(item: VaultEntry, secret: String): String {
    val label = URLEncoder.encode(item.username.ifBlank { "Account" }, "UTF-8").replace("+", "%20")
    val issuer = URLEncoder.encode(item.title, "UTF-8").replace("+", "%20")
    return "otpauth://totp/$issuer:$label?secret=$secret&issuer=$issuer&digits=${item.totpDigits}&period=${item.totpPeriod}&algorithm=${item.totpAlgorithm}"
}
