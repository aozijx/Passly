package com.example.poop.features.vault.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.example.poop.R
import com.example.poop.core.common.AddType
import com.example.poop.core.crypto.CryptoManager
import com.example.poop.core.designsystem.base.BaseVaultDialog
import com.example.poop.core.designsystem.fields.CategoryDropdown
import com.example.poop.core.designsystem.fields.PasswordInput
import com.example.poop.core.designsystem.fields.VaultTextField
import com.example.poop.core.designsystem.state.PasswordAddState
import com.example.poop.data.model.VaultEntry
import com.example.poop.features.vault.VaultViewModel

@Composable
fun AddPasswordDialog(
    viewModel: VaultViewModel
) {
    val state = remember { PasswordAddState() }
    val unfiledCategory = stringResource(R.string.category_unfiled)

    BaseVaultDialog(
        title = stringResource(R.string.vault_add_password_title),
        onDismiss = { viewModel.addType = AddType.NONE },
        confirmEnabled = state.isValid,
        onConfirm = {
            val encUser = CryptoManager.encrypt(state.username)
            val encPass = CryptoManager.encrypt(state.password)
            
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
