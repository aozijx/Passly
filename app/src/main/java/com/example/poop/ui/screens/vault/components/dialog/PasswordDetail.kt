package com.example.poop.ui.screens.vault.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.components.common.CategoryItem
import com.example.poop.ui.screens.vault.components.common.DetailActions
import com.example.poop.ui.screens.vault.components.common.DetailHeader
import com.example.poop.ui.screens.vault.components.common.DetailItem
import com.example.poop.ui.screens.vault.components.common.EditTextField
import com.example.poop.ui.screens.vault.utils.VaultSecurityUtils
import com.example.poop.util.ClipboardUtils

@Composable
fun PasswordDetailDialog(
    activity: FragmentActivity,
    item: VaultEntry,
    viewModel: VaultViewModel
) {
    // 瞬时状态管理
    var revealedUsername by remember { mutableStateOf<String?>(null) }
    var revealedPassword by remember { mutableStateOf<String?>(null) }
    var showAdvancedSettings by remember { mutableStateOf(false) }

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
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CategoryItem(viewModel, item)

                // 账号凭据区域
                CredentialSection(
                    activity = activity,
                    item = item,
                    viewModel = viewModel,
                    revealedUsername = revealedUsername,
                    revealedPassword = revealedPassword,
                    onUsernameRevealed = { revealedUsername = it },
                    onPasswordRevealed = { revealedPassword = it }
                )
            }
        },
        confirmButton = {
            DetailActions(
                onDeleteClick = { viewModel.requestDelete(item) },
                onDismiss = { viewModel.dismissDetail() }
            )
        }
    )
}

@Composable
private fun CredentialSection(
    activity: FragmentActivity,
    item: VaultEntry,
    viewModel: VaultViewModel,
    revealedUsername: String?,
    revealedPassword: String?,
    onUsernameRevealed: (String?) -> Unit,
    onPasswordRevealed: (String?) -> Unit
) {
    val context = LocalContext.current
    val showPassword = item.password.isNotEmpty() || item.entryType != 1

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 账号
        if (viewModel.isEditingUsername) {
            EditTextField(
                value = viewModel.editedUsername,
                onValueChange = { viewModel.editedUsername = it },
                label = "修改账号",
                onSave = {
                    val newValue = viewModel.editedUsername
                    VaultSecurityUtils.encryptMultiple(activity, listOf(newValue), title = "保存修改") { results ->
                        viewModel.saveUsernameEdit(results[0])
                        onUsernameRevealed(newValue)
                    }
                }
            )
        } else {
            DetailItem(
                label = "账号",
                value = if (item.username.isEmpty() && item.entryType == 1) "未关联账号" else (revealedUsername ?: "••••••••"),
                isRevealed = revealedUsername != null,
                onCopy = {
                    if (item.username.isNotEmpty()) {
                        VaultSecurityUtils.decryptSingle(activity, item.username) { it?.let { ClipboardUtils.copy(context, it) } }
                    }
                },
                onEdit = {
                    val current = revealedUsername ?: ""
                    viewModel.startEditingUsername(current)
                }
            )
        }

        // 密码
        if (showPassword) {
            if (viewModel.isEditingPassword) {
                EditTextField(
                    value = viewModel.editedPassword,
                    onValueChange = { viewModel.editedPassword = it },
                    label = "修改密码",
                    onSave = {
                        val newValue = viewModel.editedPassword
                        VaultSecurityUtils.encryptMultiple(activity, listOf(newValue), title = "保存修改") { results ->
                            viewModel.savePasswordEdit(results[0])
                            onPasswordRevealed(newValue)
                        }
                    }
                )
            } else {
                DetailItem(
                    label = "密码",
                    value = revealedPassword ?: "••••••••",
                    isRevealed = revealedPassword != null,
                    onCopy = {
                        VaultSecurityUtils.decryptSingle(activity, item.password) { it?.let { ClipboardUtils.copy(context, it) } }
                    },
                    onEdit = { revealedPassword?.let { viewModel.startEditingPassword(it) } }
                )
            }
        }

        // 显隐按钮逻辑
        val hasHiddenContent = (revealedUsername == null && item.username.isNotEmpty()) || (revealedPassword == null && showPassword && item.password.isNotEmpty())

        if (hasHiddenContent && !viewModel.isEditingUsername && !viewModel.isEditingPassword) {
            Button(
                onClick = {
                    val fieldsToDecrypt = mutableListOf<String>()
                    if (item.username.isNotEmpty()) fieldsToDecrypt.add(item.username)
                    if (item.password.isNotEmpty() && showPassword) fieldsToDecrypt.add(item.password)

                    VaultSecurityUtils.decryptMultiple(activity, fieldsToDecrypt) { results ->
                        var idx = 0
                        if (item.username.isNotEmpty()) onUsernameRevealed(results[idx++])
                        if (item.password.isNotEmpty() && showPassword) onPasswordRevealed(results[idx++])
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("验证并显示详情")
            }
        } else if (!hasHiddenContent && (revealedUsername != null || (revealedPassword != null && showPassword)) && !viewModel.isEditingUsername && !viewModel.isEditingPassword) {
            TextButton(
                onClick = { onUsernameRevealed(null); onPasswordRevealed(null) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.VisibilityOff, contentDescription = null); Text("隐藏敏感信息")
            }
        }
    }
}
