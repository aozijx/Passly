package com.example.poop.features.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.poop.MainViewModel
import com.example.poop.core.designsystem.DetailActions
import com.example.poop.core.designsystem.DetailHeader
import com.example.poop.core.designsystem.sections.CategoryItem
import com.example.poop.core.designsystem.sections.CredentialSection
import com.example.poop.core.designsystem.state.VaultEditState
import com.example.poop.data.model.VaultEntry
import com.example.poop.features.vault.VaultViewModel

@Composable
fun PasswordDetailDialog(
    activity: FragmentActivity,
    item: VaultEntry,
    vaultViewModel: VaultViewModel,
    mainViewModel: MainViewModel
) {
    var revealedUsername by remember { mutableStateOf<String?>(null) }
    var revealedPassword by remember { mutableStateOf<String?>(null) }
    
    val editState = remember(item) { VaultEditState(item) }

    AlertDialog(
        onDismissRequest = { vaultViewModel.dismissDetail() },
        title = {
            DetailHeader(
                item = item,
                onIconClick = { vaultViewModel.showIconPicker = true }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                CategoryItem(vaultViewModel, item, editState)

                CredentialSection(
                    activity = activity,
                    item = item,
                    vaultViewModel = vaultViewModel,
                    mainViewModel = mainViewModel,
                    editState = editState,
                    revealedUsername = revealedUsername,
                    revealedPassword = revealedPassword,
                    onUsernameRevealed = { revealedUsername = it },
                    onPasswordRevealed = { revealedPassword = it }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        },
        confirmButton = {
            DetailActions(onDeleteClick = { vaultViewModel.itemToDelete = item }, onDismiss = { vaultViewModel.dismissDetail() })
        }
    )
}
