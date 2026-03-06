package com.example.poop.ui.screens.vault.twoFA

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.utils.CryptoManager
import com.example.poop.util.ClipboardUtils
import com.example.poop.util.Logcat
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTwoFADialog(
    activity: FragmentActivity,
    viewModel: VaultViewModel
) {
    val context = LocalContext.current
    val algorithms = listOf("SHA1", "SHA256", "SHA512", "STEAM")
    var uriText by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }

    // 自动解析 otpauth URI
    LaunchedEffect(uriText) {
        if (!uriText.startsWith("otpauth://totp/")) return@LaunchedEffect
        try {
            val uri = uriText.toUri()
            val label = URLDecoder.decode(uri.path?.trimStart('/') ?: "", "UTF-8")
            val secret = uri.getQueryParameter("secret") ?: ""
            val issuer = uri.getQueryParameter("issuer")
            val rawAlgorithm = uri.getQueryParameter("algorithm")?.uppercase()
            val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6
            val period = uri.getQueryParameter("period")?.toIntOrNull() ?: 30

            viewModel.addDialogTitle = issuer ?: label.split(":").firstOrNull() ?: ""
            viewModel.addDialogUsername = label
            viewModel.addDialogTotpSecret = secret

            // 检测 Steam
            val checkText = issuer ?: label
            viewModel.detectSteam(checkText, false)

            // 如果不是 Steam，则应用解析出的算法 and 位数
            if (viewModel.addDialogTotpAlgorithm != "STEAM") {
                val algorithm = rawAlgorithm?.takeIf { algorithms.contains(it) } ?: "SHA1"
                viewModel.addDialogTotpAlgorithm = algorithm
                viewModel.addDialogTotpDigits = digits.toString()
            }
            viewModel.addDialogTotpPeriod = period.toString()

            Toast.makeText(context, "已解析 URI 并自动清理剪贴板", Toast.LENGTH_SHORT).show()
            // 解析成功后立即清除剪贴板，保护敏感 URI
            ClipboardUtils.clear(context)
        } catch (e: Exception) {
            Logcat.e("AddTwoFA", "URI 解析失败", e)
            Toast.makeText(context, "URI 解析失败", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = { viewModel.dismissAddDialog() },
        title = { Text("新增 2FA 令牌", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = viewModel.addDialogTitle,
                    onValueChange = {
                        viewModel.addDialogTitle = it
                        viewModel.detectSteam(it, false)
                    },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uriText,
                    onValueChange = { uriText = it },
                    label = { Text("粘贴 otpauth:// URI") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { uriText = ClipboardUtils.getText(context) }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "粘贴")
                        }
                    }
                )
                OutlinedTextField(
                    value = viewModel.addDialogCategory,
                    onValueChange = { viewModel.addDialogCategory = it },
                    label = { Text("分类") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例如：社交、工作") }
                )

                if (showAdvanced) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    AddTwoFASection(
                        activity = activity,
                        item = VaultEntry(
                            title = viewModel.addDialogTitle,
                            username = viewModel.addDialogUsername,
                            password = "",
                            category = viewModel.addDialogCategory,
                            totpSecret = viewModel.addDialogTotpSecret,
                            totpDigits = viewModel.addDialogTotpDigits.toIntOrNull() ?: 6,
                            totpPeriod = viewModel.addDialogTotpPeriod.toIntOrNull() ?: 30,
                            totpAlgorithm = viewModel.addDialogTotpAlgorithm,
                            entryType = 1
                        ),
                        viewModel = viewModel,
                        revealedSecret = viewModel.addDialogTotpSecret
                    )
                } else {
                    TextButton(onClick = { showAdvanced = true }) {
                        Text("高级配置")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalSecret = if (showAdvanced) viewModel.editedTotpSecret else viewModel.addDialogTotpSecret
                val finalAlgorithm = if (showAdvanced) viewModel.editedTotpAlgorithm else viewModel.addDialogTotpAlgorithm
                val finalDigits = if (showAdvanced) viewModel.editedTotpDigits else viewModel.addDialogTotpDigits
                val finalPeriod = if (showAdvanced) viewModel.editedTotpPeriod else viewModel.addDialogTotpPeriod

                if (viewModel.addDialogTitle.isBlank() || finalSecret.isBlank()) {
                    Toast.makeText(context, "请完善必要信息", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // 使用免验证密钥加密 TOTP 密钥，以便静默刷新
                val cipher = CryptoManager.getEncryptCipher(isSilent = true)
                if (cipher != null) {
                    val encryptedSecret = CryptoManager.encrypt(finalSecret.trim(), cipher)
                    val entry = VaultEntry(
                        title = viewModel.addDialogTitle,
                        username = viewModel.addDialogUsername,
                        password = "",
                        category = viewModel.addDialogCategory,
                        totpSecret = encryptedSecret,
                        totpDigits = finalDigits.toIntOrNull() ?: 6,
                        totpPeriod = finalPeriod.toIntOrNull() ?: 30,
                        totpAlgorithm = finalAlgorithm,
                        entryType = 1
                    )
                    viewModel.addItem(entry)
                    viewModel.dismissAddDialog()
                } else {
                    Toast.makeText(context, "加密失败，请重试", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissAddDialog() }) { Text("取消") }
        }
    )
}
