package com.aozijx.passly.ui.features.vault.components

import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.ui.components.AppDialog
import com.aozijx.passly.ui.components.AppTextField
import com.aozijx.passly.ui.components.PasswordInput
import com.aozijx.passly.ui.features.vault.VaultViewModel
import com.aozijx.passly.ui.features.vault.model.AddType

@Composable
fun AddGenericEntryDialog(
    viewModel: VaultViewModel,
    addType: AddType,
    onUpdateInteraction: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val state = remember { GenericAddState() }
    val unfiledCategory = stringResource(R.string.category_unfiled)
    val typeLabel = stringResource(addType.labelRes)
    val entryTypeValue = when (addType) {
        AddType.BANK_CARD -> 5
        AddType.WIFI -> 3
        AddType.SSH_KEY -> 8
        AddType.ID_CARD -> 7
        AddType.SEED_PHRASE -> 6
        AddType.PASSKEY -> 2
        AddType.RECOVERY_CODE -> 4
        else -> 0
    }

    AppDialog(
        title = stringResource(R.string.vault_add_generic_title, typeLabel),
        onDismiss = { viewModel.setAddType(null) },
        confirmEnabled = state.title.isNotBlank(),
        onConfirm = {
            try {
                val entry = VaultEntry(
                    title = state.title,
                    username = state.username,
                    password = state.password,
                    category = state.category.ifBlank { unfiledCategory },
                    notes = state.notes.ifBlank { null },
                    entryType = entryTypeValue
                )
                viewModel.addItem(entry)
                viewModel.setAddType(null)
            } catch (e: Exception) {
                Logcat.e("AddGenericEntry", "Failed to save", e)
                Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
            }
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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        AppTextField(
            value = state.notes,
            onValueChange = { state.notes = it; onUpdateInteraction() },
            label = stringResource(R.string.remark),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        CategoryDropdown(
            selectedCategory = state.category,
            onCategorySelected = { state.category = it },
            availableCategories = uiState.availableCategories
        )
    }
}