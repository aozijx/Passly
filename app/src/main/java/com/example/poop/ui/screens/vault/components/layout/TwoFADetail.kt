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
import coil.request.ImageRequest
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.components.common.VaultItemIcon
import com.example.poop.ui.screens.vault.components.dialog.IconPickerDialog
import com.example.poop.ui.screens.vault.components.items.TwoFASection
import com.example.poop.ui.screens.vault.utils.VaultSecurityUtils
import com.example.poop.util.ClipboardUtils

@Composable
fun TwoFADetailDialog(
    activity: FragmentActivity,
    item: VaultEntry,
    viewModel: VaultViewModel
) {
    var revealedUsername by remember { mutableStateOf<String?>(null) }
    var revealed2faSecret by remember { mutableStateOf<String?>(null) }
    var revealedNotes by remember { mutableStateOf<String?>(null) }
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

                // 2FA 动态码区域 (核心)
                if (revealed2faSecret != null) {
                    TwoFASection(item = item, secret = revealed2faSecret!!)
                } else {
                    Button(
                        onClick = {
                            VaultSecurityUtils.decryptSingle(activity, item.totpSecret ?: "") { revealed2faSecret = it }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("验证并显示 2FA 动态码")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 账号信息 (辅助)
                if (viewModel.isEditingUsername) {
                    EditTextField(
                        value = viewModel.editedUsername,
                        onValueChange = { viewModel.editedUsername = it },
                        label = "修改账号",
                        onSave = {
                            val newValue = viewModel.editedUsername
                            VaultSecurityUtils.encryptMultiple(activity, listOf(newValue), title = "保存修改") { results ->
                                viewModel.saveUsernameEdit(results[0])
                                revealedUsername = newValue
                            }
                        }
                    )
                } else {
                    DetailItem(
                        label = "关联账号",
                        value = if (item.username.isEmpty()) "未设置账号" else (revealedUsername ?: "••••••••"),
                        isRevealed = revealedUsername != null,
                        onCopy = {
                            if (item.username.isNotEmpty()) {
                                VaultSecurityUtils.decryptSingle(activity, item.username) { it?.let { ClipboardUtils.copy(context = activity, it) } }
                            }
                        },
                        onEdit = { 
                            val current = revealedUsername ?: ""
                            viewModel.startEditingUsername(current)
                        }
                    )
                }

                // 备注展示
                if (!item.notes.isNullOrEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DetailItem(
                        label = "备注",
                        value = revealedNotes ?: "••••••••",
                        isRevealed = revealedNotes != null,
                        onCopy = {
                            revealedNotes?.let { ClipboardUtils.copy(activity, it) }
                        },
                        onEdit = { /* 暂时不支持直接编辑备注 */ }
                    )
                }

                // 统一的详情显示按钮 (如果没有全部显示)
                val needsReveal = (revealedUsername == null && item.username.isNotEmpty()) || (revealedNotes == null && !item.notes.isNullOrEmpty())
                if (needsReveal) {
                    TextButton(
                        onClick = {
                            val toDecrypt = mutableListOf<String>()
                            if (revealedUsername == null && item.username.isNotEmpty()) toDecrypt.add(item.username)
                            if (revealedNotes == null && !item.notes.isNullOrEmpty()) toDecrypt.add(
                                item.notes
                            )
                            
                            VaultSecurityUtils.decryptMultiple(activity, toDecrypt) { results ->
                                var idx = 0
                                if (revealedUsername == null && item.username.isNotEmpty()) revealedUsername = results[idx++]
                                if (revealedNotes == null && !item.notes.isNullOrEmpty()) revealedNotes = results[idx++]
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("验证并显示账号/备注详情")
                    }
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
    val context = LocalContext.current
    if (!item.iconCustomPath.isNullOrEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onIconClick)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.iconCustomPath)
                    .allowHardware(false)
                    .crossfade(true)
                    .build(),
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
