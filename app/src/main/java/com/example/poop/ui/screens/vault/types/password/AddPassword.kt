package com.example.poop.ui.screens.vault.types.password

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.example.poop.R
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.common.base.BaseVaultDialog
import com.example.poop.ui.screens.vault.common.fields.CategoryDropdown
import com.example.poop.ui.screens.vault.common.fields.PasswordInput
import com.example.poop.ui.screens.vault.common.fields.VaultTextField
import com.example.poop.ui.screens.vault.utils.CryptoManager

@Composable
fun AddPasswordDialog(
    viewModel: VaultViewModel
) {
    val state = remember { PasswordAddState() }
    val unfiledCategory = stringResource(R.string.category_unfiled)

    BaseVaultDialog(
        title = stringResource(R.string.vault_add_password_title),
        onDismiss = { viewModel.dismissAddDialog() },
        confirmEnabled = state.isValid,
        onConfirm = {
            // 直接使用静默加密，不再通过 viewModel.encryptMultiple 触发验证
            val encUser = CryptoManager.encrypt(state.username, isSilent = true) ?: ""
            val encPass = CryptoManager.encrypt(state.password, isSilent = true) ?: ""
            
            val entry = VaultEntry(
                title = state.title,
                username = encUser,
                password = encPass,
                category = state.category.ifBlank { unfiledCategory },
                entryType = 0
            )
            viewModel.addItem(entry)
        }
    ) {
        VaultTextField(
            value = state.title,
            onValueChange = { state.title = it },
            label = stringResource(R.string.label_title_hint),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        VaultTextField(
            value = state.username,
            onValueChange = { state.username = it },
            label = stringResource(R.string.label_username_hint),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        PasswordInput(
            password = state.password,
            onPasswordChange = { state.password = it },
            isVisible = state.isPasswordVisible,
            onVisibilityChange = { state.isPasswordVisible = it },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        CategoryDropdown(
            selectedCategory = state.category,
            onCategorySelected = { state.category = it },
            availableCategories = viewModel.availableCategories.collectAsState().value
        )
    }
}
