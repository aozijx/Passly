package com.aozijx.passly.feature.vault.components.editor

import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.aozijx.passly.R
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.ui.components.AppDialog
import com.aozijx.passly.core.ui.components.AppTextField
import com.aozijx.passly.core.ui.components.PasswordInput
import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.secret.CardSecret
import com.aozijx.passly.domain.entry.model.secret.IdentitySecret
import com.aozijx.passly.domain.entry.model.secret.LoginSecret
import com.aozijx.passly.domain.entry.model.secret.PasskeySecret
import com.aozijx.passly.domain.entry.model.secret.SshSecret
import com.aozijx.passly.domain.entry.model.secret.WifiSecret
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.feature.vault.model.AddType

@Composable
fun AddGenericEntryDialog(
    viewModel: VaultViewModel,
    addType: AddType,
    onUpdateInteraction: () -> Unit
) {
    val context = LocalContext.current
    val state = remember { GenericAddState() }
    val typeLabel = stringResource(addType.labelRes)
    val entryTypeValue = when (addType) {
        AddType.BANK_CARD -> EntryType.BANK_CARD
        AddType.WIFI -> EntryType.WIFI
        AddType.SSH_KEY -> EntryType.SSH_KEY
        AddType.ID_CARD -> EntryType.ID_CARD
        AddType.SEED_PHRASE -> EntryType.SEED_PHRASE
        AddType.PASSKEY -> EntryType.PASSKEY
        AddType.RECOVERY_CODE -> EntryType.RECOVERY_CODE
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
                    secretFor(
                        type = entryTypeValue,
                        value = state.password,
                        notes = state.notes.ifBlank { null }
                    )
                )
                viewModel.addItem(entry)
                viewModel.setAddType(null)
            } catch (e: Exception) {
                AppTelemetry.e("AddGenericEntry", "Failed to save", e)
                Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
            }
        }) {
        AppTextField(
            value = state.title,
            onValueChange = { state.title = it; onUpdateInteraction() },
            label = stringResource(R.string.title),
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

    }
}

private fun secretFor(type: EntryType, value: String, notes: String?): EntrySecret = when (type) {
    EntryType.BANK_CARD, EntryType.CARD ->
        EntrySecret(card = CardSecret(cardNumber = value), notes = notes)

    EntryType.WIFI ->
        EntrySecret(wifi = WifiSecret(password = value), notes = notes)

    EntryType.SSH_KEY ->
        EntrySecret(ssh = SshSecret(privateKey = value), notes = notes)

    EntryType.ID_CARD, EntryType.IDENTITY, EntryType.PASSPORT, EntryType.LICENSE ->
        EntrySecret(identity = IdentitySecret(idNumber = value), notes = notes)

    EntryType.SEED_PHRASE ->
        EntrySecret(identity = IdentitySecret(seedPhrase = value), notes = notes)

    EntryType.RECOVERY_CODE ->
        EntrySecret(
            identity = IdentitySecret(
                recoveryCodes = value.lines().map(String::trim).filter(String::isNotEmpty)
            ),
            notes = notes
        )

    EntryType.PASSKEY ->
        EntrySecret(passkey = PasskeySecret(privateKeyReference = value), notes = notes)

    else -> EntrySecret(login = LoginSecret(password = value), notes = notes)
}
