package com.example.poop.ui.screens.vault.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.utils.VaultSecurityUtils

@Composable
fun AddPasswordDialog(
    activity: FragmentActivity,
    viewModel: VaultViewModel
) {
    AlertDialog(
        onDismissRequest = { viewModel.dismissAddDialog() },
        title = { Text("新增密码项", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = viewModel.addDialogTitle, 
                    onValueChange = { viewModel.addDialogTitle = it }, 
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )
                )
                
                OutlinedTextField(
                    value = viewModel.addDialogUsername, 
                    onValueChange = { viewModel.addDialogUsername = it }, 
                    label = { Text("用户名 / 手机号") }, 
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )
                
                OutlinedTextField(
                    value = viewModel.addDialogPassword, 
                    onValueChange = { viewModel.addDialogPassword = it }, 
                    label = { Text("密码") },
                    visualTransformation = if (viewModel.addDialogPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.addDialogPasswordVisible = !viewModel.addDialogPasswordVisible }) {
                            Icon(
                                imageVector = if (viewModel.addDialogPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "显示密码"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    )
                )
                
                OutlinedTextField(
                    value = viewModel.addDialogCategory,
                    onValueChange = { viewModel.addDialogCategory = it },
                    label = { Text("自定义分类") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入分类名称") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if(viewModel.addDialogTitle.isNotBlank()) {
                        // 普通密码项
                        VaultSecurityUtils.encryptMultiple(activity, listOf(viewModel.addDialogUsername, viewModel.addDialogPassword)) { results ->
                            viewModel.addItem(
                                viewModel.addDialogTitle, 
                                results[0], 
                                results[1],
                                viewModel.addDialogCategory.ifBlank { "未分类" }
                            )
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
