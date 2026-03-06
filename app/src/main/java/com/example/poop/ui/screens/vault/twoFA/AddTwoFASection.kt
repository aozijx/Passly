package com.example.poop.ui.screens.vault.twoFA

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.utils.VaultSecurityUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTwoFASection(
    activity: FragmentActivity,
    item: VaultEntry,
    viewModel: VaultViewModel,
    revealedSecret: String?
) {
    var algorithmExpanded by remember { mutableStateOf(false) }
    var secretVisible by remember { mutableStateOf(false) }
    val algorithms = listOf("SHA1", "SHA256", "SHA512", "STEAM")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "2FA 配置",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            if (!viewModel.isEditingTotpConfig) {
                IconButton(
                    onClick = { revealedSecret?.let { viewModel.startEditingTotp(it) } },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "编辑配置",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (viewModel.isEditingTotpConfig) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.editedTotpSecret,
                    onValueChange = { viewModel.editedTotpSecret = it },
                    label = { Text("密钥") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    visualTransformation = if (secretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { secretVisible = !secretVisible }) {
                            Icon(
                                if (secretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (secretVisible) "隐藏密钥" else "显示密钥"
                            )
                        }
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viewModel.editedTotpDigits,
                        onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.editedTotpDigits = it },
                        label = { Text("位数") },
                        modifier = Modifier.weight(1f),
                        enabled = viewModel.editedTotpAlgorithm != "STEAM",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = viewModel.editedTotpPeriod,
                        onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.editedTotpPeriod = it },
                        label = { Text("周期") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = algorithmExpanded,
                    onExpandedChange = { algorithmExpanded = !algorithmExpanded }
                ) {
                    OutlinedTextField(
                        value = viewModel.editedTotpAlgorithm,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("算法") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = algorithmExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = algorithmExpanded,
                        onDismissRequest = { algorithmExpanded = false }
                    ) {
                        algorithms.forEach { algo ->
                            DropdownMenuItem(
                                text = { Text(algo) },
                                onClick = {
                                    viewModel.editedTotpAlgorithm = algo
                                    if (algo == "STEAM") {
                                        viewModel.editedTotpDigits = "5"
                                        viewModel.editedTotpPeriod = "30"
                                    }
                                    algorithmExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (viewModel.detailItem != null) {
                            // 编辑模式：加密并保存到数据库
                            VaultSecurityUtils.encryptMultiple(activity, listOf(viewModel.editedTotpSecret), title = "保存配置") {
                                viewModel.saveTotpEdit(it[0])
                            }
                        } else {
                            // 新增模式：将配置应用回 addDialog 状态
                            viewModel.addDialogTotpSecret = viewModel.editedTotpSecret
                            viewModel.addDialogTotpAlgorithm = viewModel.editedTotpAlgorithm
                            viewModel.addDialogTotpDigits = viewModel.editedTotpDigits
                            viewModel.addDialogTotpPeriod = viewModel.editedTotpPeriod
                            viewModel.isEditingTotpConfig = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (viewModel.detailItem != null) "保存配置" else "应用配置")
                }
            }
        } else {
            // 只读展示
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ConfigRow("算法", item.totpAlgorithm)
                ConfigRow("位数", item.totpDigits.toString())
                ConfigRow("周期", "${item.totpPeriod}s")
            }
        }
    }
}

@Composable
private fun ConfigRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
