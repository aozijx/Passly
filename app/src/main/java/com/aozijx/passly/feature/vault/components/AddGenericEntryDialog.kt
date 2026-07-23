package com.aozijx.passly.feature.vault.components

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
import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.domain.model.entry.EntryHeader
import com.aozijx.passly.domain.model.entry.EntryId
import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.EntrySummary
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.EntryVersion
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.secret.LoginSecret
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.ui.components.AppDialog
import com.aozijx.passly.ui.components.AppTextField
import com.aozijx.passly.ui.components.PasswordInput

@Composable
fun AddGenericEntryDialog(
    viewModel: VaultViewModel,
    addType: AddType,
    onUpdateInteraction: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val state = remember { GenericAddState() }
    val typeLabel = stringResource(addType.labelRes)
    val entryTypeValue = when (addType) {
        AddType.BANK_CARD -> EntryType.BANK_CARD
        AddType.WIFI -> EntryType.WIFI
        AddType.SSH_KEY -> EntryType.SSH_KEY
        AddType.ID_CARD -> EntryType.ID_CARD
        AddType.SEED_PHRASE -> EntryType.LOGIN
        AddType.PASSKEY -> EntryType.LOGIN
        AddType.RECOVERY_CODE -> EntryType.LOGIN
        else -> EntryType.LOGIN
    }

    AppDialog(
        title = stringResource(R.string.vault_add_generic_title, typeLabel),
        onDismiss = { viewModel.setAddType(null) },
        confirmEnabled = state.title.isNotBlank(),
        onConfirm = {
            try {
                val entry = VaultEntry(
                    EntryHeader(
                        id = EntryId(""),
                        entryType = entryTypeValue,
                        version = EntryVersion.INITIAL,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ),
                    EntrySummary(
                        title = state.title,
                        username = state.username,
                        icon = null
                    ),
                    EntrySecret(
                        login = LoginSecret(
                            password = state.password
                        ),
                        notes = state.notes.ifBlank { null }
                    )
                )
                viewModel.addItem(entry)
                viewModel.setAddType(null)
            } catch (e: Exception) {
                AppLog.e("AddGenericEntry", "Failed to save", e)
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