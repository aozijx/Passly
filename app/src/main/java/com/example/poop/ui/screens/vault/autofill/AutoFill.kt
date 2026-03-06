package com.example.poop.ui.screens.vault.autofill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.poop.ui.screens.vault.components.common.CategoryItem
import com.example.poop.ui.screens.vault.components.common.DetailActions
import com.example.poop.ui.screens.vault.components.common.DetailHeader
import com.example.poop.ui.screens.vault.components.common.DetailItem
import com.example.poop.ui.screens.vault.components.dialog.CredentialSection
import com.example.poop.util.ClipboardUtils

@Composable
fun AutoFillDetailDialog(
    item: VaultEntry,
    viewModel: VaultViewModel,
    activity: FragmentActivity
) {
    val context = LocalContext.current
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

                // 域名详情：有就显示，没有就不显示（除非正在编辑）
                if (viewModel.isEditingDomain) {
                    OutlinedTextField(
                        value = viewModel.editedDomain,
                        onValueChange = { viewModel.editedDomain = it },
                        label = { Text("关联域名") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { viewModel.saveDomainEdit(viewModel.editedDomain) }) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.isEditingDomain = false }) {
                                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    )
                } else if (!item.associatedDomain.isNullOrBlank()) {
                    DetailItem(
                        label = "关联域名",
                        value = item.associatedDomain,
                        isRevealed = true,
                        onCopy = { ClipboardUtils.copy(context, item.associatedDomain) },
                        onEdit = { viewModel.startEditingDomain(item.associatedDomain) }
                    )
                }

                // 包名详情：有就显示，没有就不显示（除非正在编辑）
                if (viewModel.isEditingPackage) {
                    OutlinedTextField(
                        value = viewModel.editedPackage,
                        onValueChange = { viewModel.editedPackage = it },
                        label = { Text("关联包名") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { viewModel.savePackageEdit(viewModel.editedPackage) }) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.isEditingPackage = false }) {
                                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    )
                } else if (!item.associatedAppPackage.isNullOrBlank()) {
                    DetailItem(
                        label = "关联包名",
                        value = item.associatedAppPackage,
                        isRevealed = true,
                        onCopy = { ClipboardUtils.copy(context, item.associatedAppPackage) },
                        onEdit = { viewModel.startEditingPackage(item.associatedAppPackage) }
                    )
                }

                if (isSilentData) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "此数据为自动抓取，建议验证后升级安全等级",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
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
            DetailActions(
                onDeleteClick = { viewModel.requestDelete(item) },
                onDismiss = { viewModel.dismissDetail() }
            )
        }
    )
}
