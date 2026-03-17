package com.example.poop.types.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.poop.MainViewModel
import com.example.poop.R
import com.example.poop.common.DetailActions
import com.example.poop.common.DetailHeader
import com.example.poop.common.sections.CategoryItem
import com.example.poop.common.sections.CredentialSection
import com.example.poop.common.state.VaultEditState
import com.example.poop.data.VaultEntry
import com.example.poop.types.autofill.rememberDecryptedAutofillData

@Composable
fun PasswordDetailDialog(
    activity: FragmentActivity,
    item: VaultEntry,
    viewModel: MainViewModel
) {
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

                if (isSilentData) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.vault_upgrade_security),
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
                    onPasswordRevealed = { revealedPassword = it },
                    onUpgraded = { isSilentData = false }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        },
        confirmButton = {
            DetailActions(onDeleteClick = { viewModel.requestDelete(item) }, onDismiss = { viewModel.dismissDetail() })
        }
    )
}
