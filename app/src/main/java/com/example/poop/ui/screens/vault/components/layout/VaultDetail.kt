package com.example.poop.ui.screens.vault.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.components.common.VaultItemIcon
import com.example.poop.ui.screens.vault.components.dialog.IconPickerDialog
import com.example.poop.ui.screens.vault.components.items.TotpSection
import com.example.poop.ui.screens.vault.utils.VaultSecurityUtils
import com.example.poop.util.ClipboardUtils

@Composable
fun VaultDetailDialog(
    activity: FragmentActivity,
    item: VaultEntry,
    viewModel: VaultViewModel
) {
    // 瞬时状态管理
    var revealedUsername by remember { mutableStateOf<String?>(null) }
    var revealedPassword by remember { mutableStateOf<String?>(null) }
    var revealedTotpSecret by remember { mutableStateOf<String?>(null) }
    
    // 图标选择器状态
    val showIconPicker = remember { mutableStateOf(false) }

    if (showIconPicker.value) {
        IconPickerDialog(
            onDismiss = { showIconPicker.value = false },
            currentIconName = item.iconName,
            currentCustomPath = item.iconCustomPath,
            onIconSelected = { newIconName ->
                viewModel.updateVaultEntry(item.copy(iconName = newIconName, iconCustomPath = null))
                showIconPicker.value = false
            },
            onCustomImageSelected = { uri ->
                viewModel.updateVaultEntry(item.copy(iconName = null, iconCustomPath = uri.toString()))
                showIconPicker.value = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = { viewModel.dismissDetail() },
        title = {
            DetailHeader(item = item, onIconClick = { showIconPicker.value = true })
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!item.iconCustomPath.isNullOrEmpty()) {
                    Text(item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                CategoryItem(viewModel, item)

                if (item.totpSecret != null) {
                    // 对于 TOTP，我们也需要先验证解密出密钥，再传给 TotpSection 生成验证码
                    if (revealedTotpSecret != null) {
                        TotpSection(item = item, secret = revealedTotpSecret!!)
                    } else {
                        Button(
                            onClick = {
                                VaultSecurityUtils.decryptSingle(activity, item.totpSecret) { revealedTotpSecret = it }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("验证身份并查看动态码")
                        }
                    }
                } else {
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
private fun DetailHeader(item: VaultEntry, onIconClick: () -> Unit) {
    if (!item.iconCustomPath.isNullOrEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onIconClick)
        ) {
            AsyncImage(
                model = item.iconCustomPath,
                contentDescription = "背景图",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).clickable(onClick = onIconClick),
                contentAlignment = Alignment.Center
            ) {
                VaultItemIcon(item = item, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(item.title, fontWeight = FontWeight.Bold)
        }
    }
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
    val isRevealed = revealedUsername != null && revealedPassword != null

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
                value = revealedUsername ?: "••••••••",
                isRevealed = revealedUsername != null,
                onCopy = {
                    VaultSecurityUtils.decryptSingle(activity, item.username) { it?.let { ClipboardUtils.copy(context, it) } }
                },
                onEdit = { revealedUsername?.let { viewModel.startEditingUsername(it) } }
            )
        }

        // 密码
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

        // 显隐按钮
        if (!isRevealed && !viewModel.isEditingUsername && !viewModel.isEditingPassword) {
            Button(
                onClick = {
                    VaultSecurityUtils.decryptMultiple(activity, listOf(item.username, item.password)) { results ->
                        if (results.size >= 2) {
                            onUsernameRevealed(results[0])
                            onPasswordRevealed(results[1])
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("验证并显示")
            }
        } else if (isRevealed && !viewModel.isEditingUsername && !viewModel.isEditingPassword) {
            TextButton(
                onClick = { onUsernameRevealed(null); onPasswordRevealed(null) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.VisibilityOff, contentDescription = null); Text("隐藏信息")
            }
        }
    }
}

@Composable
private fun DetailActions(onDeleteClick: () -> Unit, onDismiss: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onDeleteClick, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.Default.Delete, null); Text("删除")
        }
        TextButton(onClick = onDismiss) { Text("关闭") }
    }
}

@Composable
private fun EditTextField(value: String, onValueChange: (String) -> Unit, label: String, onSave: () -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        trailingIcon = { IconButton(onClick = onSave) { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) } }
    )
}

@Composable
private fun CategoryItem(viewModel: VaultViewModel, entry: VaultEntry) {
    if (viewModel.isEditingCategory) {
        EditTextField(value = viewModel.editedCategory, onValueChange = { viewModel.editedCategory = it }, label = "修改分类", onSave = { viewModel.saveCategoryEdit() })
    } else {
        Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.startEditingCategory() }.padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("分类", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            Text(entry.category, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String, isRevealed: Boolean, onCopy: () -> Unit, onEdit: () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(value, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), letterSpacing = if (isRevealed) 0.sp else 2.sp)
            IconButton(onClick = if (isRevealed) onEdit else onCopy, modifier = Modifier.size(32.dp)) {
                Icon(if (isRevealed) Icons.Default.Edit else Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
