package com.aozijx.passly.features.vault.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.designsystem.AppDialog
import com.aozijx.passly.core.designsystem.AppTextField
import com.aozijx.passly.core.designsystem.PasswordInput
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.vault.VaultViewModel

@Composable
fun AddPasswordDialog(
    viewModel: VaultViewModel,
    onUpdateInteraction: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = remember { PasswordAddState() }
    val unfiledCategory = stringResource(R.string.category_unfiled)

    AppDialog(
        title = stringResource(R.string.vault_add_password_title),
        onDismiss = { viewModel.setAddType(null) },
        confirmEnabled = state.isValid,
        onConfirm = {
            val entry = VaultEntry(
                title = state.title,
                username = state.username,
                password = state.password,
                category = state.category.ifBlank { unfiledCategory },
                entryType = 0
            )
            viewModel.addItem(entry)
        }) {
        AppTextField(
            value = state.title,
            onValueChange = { state.title = it; onUpdateInteraction() },
            label = stringResource(R.string.label_title_hint),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        AppTextField(
            value = state.username,
            onValueChange = { state.username = it; onUpdateInteraction() },
            label = stringResource(R.string.label_username_hint),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        PasswordInput(
            password = state.password,
            onPasswordChange = { state.password = it; onUpdateInteraction() },
            isVisible = state.isPasswordVisible,
            onVisibilityChange = { state.isPasswordVisible = it },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        CategoryDropdown(
            selectedCategory = state.category,
            onCategorySelected = { state.category = it },
            availableCategories = uiState.availableCategories
        )
    }
}