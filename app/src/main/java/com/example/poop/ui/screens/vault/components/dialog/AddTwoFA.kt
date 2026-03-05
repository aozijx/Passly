package com.example.poop.ui.screens.vault.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.utils.VaultSecurityUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTwoFADialog(
    activity: FragmentActivity,
    viewModel: VaultViewModel
) {
    var algorithmExpanded by remember { mutableStateOf(false) }
    val algorithms = listOf("SHA1", "SHA256", "SHA512", "STEAM")

    AlertDialog(
        onDismissRequest = { viewModel.dismissAddDialog() },
        title = { Text("新增 2FA 令牌", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = viewModel.addDialogTitle,
                    onValueChange = { viewModel.addDialogTitle = it },
                    label = { Text("发行者 (如: Google, Steam)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.addDialogUsername,
                    onValueChange = { viewModel.addDialogUsername = it },
                    label = { Text("账号名称") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.addDialogTotpSecret,
                    onValueChange = { viewModel.addDialogTotpSecret = it },
                    label = { Text("秘钥 (Secret Key)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Base32 编码的密钥") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.addDialogTotpDigits,
                        onValueChange = { if (it.all { char -> char.isDigit() }) viewModel.addDialogTotpDigits = it },
                        label = { Text("位数") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = viewModel.addDialogTotpAlgorithm != "STEAM"
                    )
                    OutlinedTextField(
                        value = viewModel.addDialogTotpPeriod,
                        onValueChange = { if (it.all { char -> char.isDigit() }) viewModel.addDialogTotpPeriod = it },
                        label = { Text("周期 (秒)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = algorithmExpanded,
                    onExpandedChange = { algorithmExpanded = !algorithmExpanded }
                ) {
                    OutlinedTextField(
                        value = viewModel.addDialogTotpAlgorithm,
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
                                    viewModel.addDialogTotpAlgorithm = algo
                                    // Steam 模式下自动设置默认值
                                    if (algo == "STEAM") {
                                        viewModel.addDialogTotpDigits = "5"
                                        viewModel.addDialogTotpPeriod = "30"
                                    }
                                    algorithmExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = viewModel.addDialogCategory,
                    onValueChange = { viewModel.addDialogCategory = it },
                    label = { Text("分类") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (viewModel.addDialogTitle.isNotBlank() && viewModel.addDialogTotpSecret.isNotBlank()) {
                        VaultSecurityUtils.encryptMultiple(activity, listOf(viewModel.addDialogTotpSecret.trim())) { results ->
                            val entry = VaultEntry(
                                title = viewModel.addDialogTitle,
                                username = viewModel.addDialogUsername,
                                password = "",
                                category = viewModel.addDialogCategory,
                                totpSecret = results[0],
                                totpDigits = if (viewModel.addDialogTotpAlgorithm == "STEAM") 5 else (viewModel.addDialogTotpDigits.toIntOrNull() ?: 6),
                                totpPeriod = viewModel.addDialogTotpPeriod.toIntOrNull() ?: 30,
                                totpAlgorithm = viewModel.addDialogTotpAlgorithm,
                                entryType = 1
                            )
                            viewModel.addItem(entry)
                        }
                    }
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissAddDialog() }) { Text("取消") }
        }
    )
}
