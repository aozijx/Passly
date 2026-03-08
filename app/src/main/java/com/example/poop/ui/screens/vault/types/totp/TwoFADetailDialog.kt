package com.example.poop.ui.screens.vault.types.totp

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.example.poop.ui.screens.vault.common.DetailActions
import com.example.poop.ui.screens.vault.common.DetailHeader
import com.example.poop.ui.screens.vault.common.sections.CategoryItem
import com.example.poop.ui.screens.vault.common.sections.TotpConfigForm
import com.example.poop.ui.screens.vault.common.state.VaultEditState
import com.example.poop.util.ClipboardUtils
import com.example.poop.util.QrCodeUtils
import java.net.URLEncoder

@Composable
fun TwoFADetailDialog(
    activity: FragmentActivity,
    item: VaultEntry,
    viewModel: VaultViewModel
) {
    val context = LocalContext.current
    val totpState = rememberTotpState(entry = item, viewModel = viewModel, autoMigrate = true)
    val isSteam = remember(item.totpAlgorithm) { item.totpAlgorithm.uppercase() == "STEAM" }
    var showQrDialog by remember { mutableStateOf(false) }
    
    // 初始化编辑状态
    val categoryEditState = remember(item) { VaultEditState(item) }
    val totpEditState = remember(item, totpState.decryptedSecret) { 
        TotpEditState(item, totpState.decryptedSecret ?: "") 
    }

    AlertDialog(
        onDismissRequest = { viewModel.dismissDetail() },
        title = {
            DetailHeader(
                item = item,
                onIconClick = { viewModel.showIconPicker = true },
                onMoreClick = {
                    if (totpEditState.isEditing) {
                        totpEditState.isEditing = false
                    } else {
                        viewModel.authenticate(
                            activity = activity,
                            title = "查看敏感信息",
                            subtitle = "请验证身份以查看密钥详情",
                            onSuccess = { 
                                totpEditState.secret = totpState.decryptedSecret ?: ""
                                totpEditState.isEditing = true 
                            }
                        )
                    }
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                CategoryItem(viewModel = viewModel, entry = item, editState = categoryEditState)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("动态验证码", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    IconButton(
                        onClick = {
                            viewModel.authenticate(
                                activity = activity,
                                title = "查看二维码",
                                subtitle = "请验证身份以导出二维码",
                                onSuccess = {
                                    totpEditState.isEditing = false
                                    showQrDialog = true
                                } 
                            )
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.QrCode, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (totpState.code.isNotEmpty() && !totpState.code.contains("-")) {
                            ClipboardUtils.copy(context, totpState.code)
                            Toast.makeText(context, "验证码已复制", Toast.LENGTH_SHORT).show()
                        }
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
                        val displayText = if (totpState.decryptedSecret == null) "验证以显示" 
                                        else (if (isSteam) totpState.code else totpState.code.chunked(3).joinToString(" "))
                        
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = if (isSteam) 4.sp else 2.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (totpState.decryptedSecret == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(24.dp))
                        CircularProgressIndicator(
                            progress = { if (totpState.decryptedSecret == null) 0f else totpState.progress },
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 4.dp,
                            color = if (totpState.progress < 0.2f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }

                if (totpEditState.isEditing && totpState.decryptedSecret != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    EditTotpSection(activity, item, viewModel, totpEditState)
                }
            }
        },
        confirmButton = {
            DetailActions(onDeleteClick = { viewModel.requestDelete(item) }, onDismiss = { viewModel.dismissDetail() })
        }
    )

    if (showQrDialog) {
        if (totpState.decryptedSecret != null) {
            val qrContent = constructOtpAuthUri(item, totpState.decryptedSecret)
            val qrBitmap = remember(qrContent) { QrCodeUtils.generateQrCode(qrContent) }
            QrExportDialog(bitmap = qrBitmap, onDismiss = { showQrDialog = false })
        }
    }
}

/**
 * TOTP 编辑区域私有组件
 */
@Composable
private fun EditTotpSection(
    activity: FragmentActivity, 
    item: VaultEntry, 
    viewModel: VaultViewModel,
    editState: TotpEditState
) {
    LaunchedEffect(editState.secret) {
        if (editState.secret.contains("Steam", ignoreCase = true)) {
            editState.applySteamPreset()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("编辑 TOTP 配置", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        TotpConfigForm(
            secret = editState.secret, onSecretChange = { editState.secret = it },
            period = editState.period, onPeriodChange = { editState.period = it },
            digits = editState.digits, onDigitsChange = { editState.digits = it },
            algorithm = editState.algorithm, onAlgorithmChange = { editState.algorithm = it }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { editState.isEditing = false }) { Text("取消") }
            Button(onClick = {
                if (editState.secret.isNotBlank()) {
                    viewModel.encryptMultiple(activity, listOf(editState.secret)) { 
                        it.firstOrNull()?.let { enc -> 
                            viewModel.updateVaultEntry(item.copy(
                                totpSecret = enc,
                                totpPeriod = editState.period.toIntOrNull() ?: 30,
                                totpDigits = editState.digits.toIntOrNull() ?: 6,
                                totpAlgorithm = editState.algorithm
                            ))
                            editState.isEditing = false
                        }
                    }
                }
            }) { Text("保存配置") }
        }
    }
}

@Composable
private fun QrExportDialog(bitmap: Bitmap?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出二维码") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), 
                horizontalAlignment = Alignment.CenterHorizontally, 
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("使用其他验证器应用扫描此二维码即可迁移此令牌。", style = MaterialTheme.typography.bodyMedium)
                if (bitmap != null) {
                    Card(modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(1f), shape = RoundedCornerShape(12.dp)) {
                        Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                            Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.fillMaxSize())
                        }
                    }
                } else CircularProgressIndicator()
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
