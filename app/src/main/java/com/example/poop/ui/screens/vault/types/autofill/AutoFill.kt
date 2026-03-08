package com.example.poop.ui.screens.vault.types.autofill

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.example.poop.R
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.common.DetailActions
import com.example.poop.ui.screens.vault.common.DetailHeader
import com.example.poop.ui.screens.vault.common.sections.CategoryItem
import com.example.poop.ui.screens.vault.common.sections.CredentialSection
import com.example.poop.ui.screens.vault.common.state.VaultEditState
import com.example.poop.util.ClipboardUtils
import com.example.poop.util.Logcat

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

    // 初始化通用的编辑状态
    val editState = remember(item) { VaultEditState(item) }

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

                CategoryItem(viewModel, item, editState)

                // 域名详情
                if (!item.associatedDomain.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { ClipboardUtils.copy(context, item.associatedDomain) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.vault_detail_associated_domain),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.associatedDomain,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            IconButton(
                                onClick = {
                                    try {
                                        val url = if (item.associatedDomain.startsWith("http")) item.associatedDomain else "https://${item.associatedDomain}"
                                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                                    } catch (e: Exception) {
                                        Logcat.e("AutoFillDetail", "Failed to open domain: ${item.associatedDomain}", e)
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // 包名详情
                if (!item.associatedAppPackage.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { ClipboardUtils.copy(context, item.associatedAppPackage) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.vault_detail_associated_package),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.associatedAppPackage,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = context.packageManager.getLaunchIntentForPackage(item.associatedAppPackage)
                                        if (intent != null) context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Logcat.e("AutoFillDetail", "Failed to launch app: ${item.associatedAppPackage}", e)
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Launch, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                if (isSilentData) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.autofill_security_upgrade_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                CredentialSection(
                    activity = activity,
                    item = item,
                    viewModel = viewModel,
                    editState = editState,
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
