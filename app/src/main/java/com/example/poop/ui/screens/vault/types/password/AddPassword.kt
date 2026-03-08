package com.example.poop.ui.screens.vault.types.password

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.ImeAction
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

    BaseVaultDialog(
        title = "添加新密码",
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
                category = state.category.ifBlank { "未分类" },
                entryType = 0
            )
            viewModel.addItem(entry)
        }
    ) {
        VaultTextField(
            value = state.title,
            onValueChange = { state.title = it },
            label = "标题 (如: Google, GitHub)",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        VaultTextField(
            value = state.username,
            onValueChange = { state.username = it },
            label = "账号/邮箱",
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
