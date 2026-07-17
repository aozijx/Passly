package com.aozijx.passly.feature.vault.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.domain.model.credential.VaultCredential
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.VaultMetadata
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.ui.components.AppDialog
import com.aozijx.passly.ui.components.AppTextField
import com.aozijx.passly.ui.components.PasswordInput

@Composable
fun AddPasswordDialog(
    viewModel: VaultViewModel,
    onUpdateInteraction: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = remember { PasswordAddState() }

    AppDialog(
        title = stringResource(R.string.vault_add_password_title),
        onDismiss = { viewModel.setAddType(null) },
        confirmEnabled = state.isValid,
        onConfirm = {
            val entry = VaultEntry(
                metadata = VaultMetadata(
                    entryId = "",
                    entryType = EntryType.LOGIN,
                    title = state.title,
                    username = state.username,
                    icon = null
                ),
                credential = VaultCredential(
                    entryId = "",
                    password = state.password
                )
            )
            viewModel.addItem(entry)
        }) {
        AppTextField(
            value = state.title,
            onValueChange = { state.title = it; onUpdateInteraction() },
            label = stringResource(R.string.title_hint),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        AppTextField(
            value = state.username,
            onValueChange = { state.username = it; onUpdateInteraction() },
            label = stringResource(R.string.username_hint),
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