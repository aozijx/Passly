package com.example.poop.ui.screens.vault.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.poop.ui.screens.vault.autofill.rememberDecryptedAutofillData
import com.example.poop.ui.screens.vault.autofill.upgradeToSecureEntry
import com.example.poop.ui.screens.vault.components.common.CategoryItem
import com.example.poop.ui.screens.vault.components.common.DetailActions
import com.example.poop.ui.screens.vault.components.common.DetailHeader
import com.example.poop.ui.screens.vault.components.common.DetailItem
import com.example.poop.ui.screens.vault.utils.VaultSecurityUtils
import com.example.poop.util.ClipboardUtils

@Composable
fun PasswordDetailDialog(
    activity: FragmentActivity,
    item: VaultEntry,
    viewModel: VaultViewModel
) {
    var revealedUsername by remember { mutableStateOf<String?>(null) }
    var revealedPassword by remember { mutableStateOf<String?>(null) }
    var isSilentData by remember { mutableStateOf(false) }

    // 1. 应用静默解密逻辑
    val (silentUser, silentPass) = rememberDecryptedAutofillData(
        entry = item,
        onReadyToUpgrade = { isSilentData = true }
    )

    // 如果是静默数据，且尚未被显式覆盖，则同步显示
    LaunchedEffect(silentUser, silentPass) {
        if (silentUser != null) revealedUsername = silentUser
        if (silentPass != null) revealedPassword = silentPass
    }

    AlertDialog(
        onDismissRequest = { viewModel.dismissDetail() },
        title = {
            DetailHeader(
                item = item,
                onIconClick = { viewModel.showIconPicker = true }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                CategoryItem(viewModel, item)

                if (isSilentData) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("此数据为自动抓取，建议验证后升级安全等级", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                CredentialSection(
                    activity = activity,
                    item = item,
                    viewModel = viewModel,
                    revealedUsername = revealedUsername,
                    revealedPassword = revealedPassword,
                    isSilentData = isSilentData,
                    onUsernameRevealed = { revealedUsername = it },
                    onPasswordRevealed = { revealedPassword = it }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        },
        confirmButton = {
            DetailActions(onDeleteClick = { viewModel.requestDelete(item) }, onDismiss = { viewModel.dismissDetail() })
        }
    )
}

@Composable
fun CredentialSection(
    activity: FragmentActivity,
    item: VaultEntry,
    viewModel: VaultViewModel,
    revealedUsername: String?,
    revealedPassword: String?,
    isSilentData: Boolean,
    onUsernameRevealed: (String?) -> Unit,
    onPasswordRevealed: (String?) -> Unit
) {
    val context = LocalContext.current
    val showPassword = item.password.isNotEmpty() || item.entryType != 1

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 账号展示
        DetailItem(
            label = "账号",
            value = revealedUsername ?: "••••••••",
            isRevealed = revealedUsername != null,
            onCopy = {
                if (revealedUsername != null) {
                    ClipboardUtils.copy(context, revealedUsername)
                } else {
                    VaultSecurityUtils.decryptSingle(activity, item.username) { it?.let { ClipboardUtils.copy(context, it) } }
                }
            },
            onEdit = { viewModel.startEditingUsername(revealedUsername ?: "") }
        )

        // 密码展示
        if (showPassword) {
            DetailItem(
                label = "密码",
                value = revealedPassword ?: "••••••••",
                isRevealed = revealedPassword != null,
                onCopy = {
                    if (revealedPassword != null) {
                        ClipboardUtils.copy(context, revealedPassword)
                    } else {
                        VaultSecurityUtils.decryptSingle(activity, item.password) { it?.let { ClipboardUtils.copy(context, it) } }
                    }
                },
                onEdit = { viewModel.startEditingPassword(revealedPassword ?: "") }
            )
        }

        // 核心按钮：验证并升级
        if ((revealedUsername == null || revealedPassword == null) || isSilentData) {
            Button(
                onClick = {
                    val fieldsToDecrypt = mutableListOf<String>()
                    if (item.username.isNotEmpty()) fieldsToDecrypt.add(item.username)
                    if (item.password.isNotEmpty()) fieldsToDecrypt.add(item.password)

                    VaultSecurityUtils.decryptMultiple(activity, fieldsToDecrypt) { results ->
                        onUsernameRevealed(results.getOrNull(0))
                        onPasswordRevealed(results.getOrNull(1))
                        
                        // 验证通过后，如果当前数据是免验证格式，自动升级为安全格式
                        if (isSilentData) {
                            upgradeToSecureEntry(item, viewModel)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(if (isSilentData) Icons.Default.Security else Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isSilentData) "验证并升级安全等级" else "验证并显示详情")
            }
        }
    }
}
