package com.aozijx.passly.feature.vault.components.editor

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.aozijx.passly.R
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.ui.components.AppDialog
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
import com.aozijx.passly.feature.vault.editor.common.EntryEditorSection
import com.aozijx.passly.feature.vault.editor.common.EntryNotesField
import com.aozijx.passly.feature.vault.editor.common.EntryPasswordField
import com.aozijx.passly.feature.vault.editor.common.EntrySecretField
import com.aozijx.passly.feature.vault.editor.common.EntryTitleField
import com.aozijx.passly.feature.vault.editor.common.EntryUsernameField
import com.aozijx.passly.feature.vault.model.AddType

@Composable
fun AddTypedEntryDialog(
    viewModel: VaultViewModel,
    addType: AddType,
    onUpdateInteraction: () -> Unit
) {
    val context = LocalContext.current
    val typeLabel = stringResource(addType.labelRes)
    val entryType = addType.toEntryType()
    val secretSpec = addType.secretFieldSpec()
    val summaryLabel = addType.summaryFieldLabel()
    val saveFailedMessage = stringResource(R.string.vault_add_entry_save_failed)

    var title by rememberSaveable(addType) { mutableStateOf("") }
    var summary by rememberSaveable(addType) { mutableStateOf("") }
    var secret by rememberSaveable(addType) { mutableStateOf("") }
    var notes by rememberSaveable(addType) { mutableStateOf("") }
    var isSecretVisible by rememberSaveable(addType) { mutableStateOf(false) }

    fun markChanged() {
        onUpdateInteraction()
    }

    AppDialog(
        title = stringResource(R.string.vault_add_generic_title, typeLabel),
        onDismiss = { viewModel.setAddType(null) },
        confirmEnabled = title.isNotBlank(),
        onConfirm = {
            try {
                val now = System.currentTimeMillis()
                val entry = VaultEntry(
                    header = EntryHeader(
                        id = EntryId(""),
                        entryType = entryType,
                        version = EntryVersion.INITIAL,
                        createdAt = now,
                        updatedAt = now
                    ),
                    summary = EntrySummary(
                        title = title,
                        username = summary,
                        icon = null
                    ),
                    secret = secretFor(
                        type = entryType,
                        value = secret,
                        notes = notes.ifBlank { null }
                    )
                )
                viewModel.addItem(entry)
                viewModel.setAddType(null)
            } catch (e: Exception) {
                AppTelemetry.e("AddTypedEntry", "Failed to save", e)
                Toast.makeText(context, saveFailedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        EntryEditorSection(title = stringResource(R.string.vault_editor_section_basic_info)) {
            EntryTitleField(
                value = title,
                onValueChange = {
                    title = it
                    markChanged()
                },
                label = stringResource(R.string.title)
            )
            EntryUsernameField(
                value = summary,
                onValueChange = {
                    summary = it
                    markChanged()
                },
                label = stringResource(summaryLabel)
            )
        }

        EntryEditorSection(title = typeLabel) {
            if (secretSpec.obscured) {
                EntryPasswordField(
                    password = secret,
                    onPasswordChange = {
                        secret = it
                        markChanged()
                    },
                    isVisible = isSecretVisible,
                    onVisibilityChange = { isSecretVisible = it },
                    imeAction = ImeAction.Next
                )
            } else {
                EntrySecretField(
                    value = secret,
                    onValueChange = {
                        secret = it
                        markChanged()
                    },
                    label = stringResource(secretSpec.labelRes),
                    keyboardType = secretSpec.keyboardType,
                    singleLine = secretSpec.singleLine,
                    imeAction = ImeAction.Next
                )
            }
        }

        EntryEditorSection(title = stringResource(R.string.vault_editor_section_details)) {
            EntryNotesField(
                value = notes,
                onValueChange = {
                    notes = it
                    markChanged()
                },
                label = stringResource(R.string.remark)
            )
        }
    }
}

private data class TypedSecretFieldSpec(
    val labelRes: Int,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val singleLine: Boolean = true,
    val obscured: Boolean = false
)

private fun AddType.toEntryType(): EntryType = when (this) {
    AddType.BANK_CARD -> EntryType.BANK_CARD
    AddType.WIFI -> EntryType.WIFI
    AddType.SSH_KEY -> EntryType.SSH_KEY
    AddType.ID_CARD -> EntryType.ID_CARD
    AddType.SEED_PHRASE -> EntryType.SEED_PHRASE
    AddType.PASSKEY -> EntryType.PASSKEY
    AddType.RECOVERY_CODE -> EntryType.RECOVERY_CODE
    else -> EntryType.LOGIN
}

private fun AddType.summaryFieldLabel(): Int = when (this) {
    AddType.BANK_CARD -> R.string.cardholder
    AddType.WIFI -> R.string.wifi_ssid
    else -> R.string.username_hint
}

private fun AddType.secretFieldSpec(): TypedSecretFieldSpec = when (this) {
    AddType.BANK_CARD -> TypedSecretFieldSpec(
        labelRes = R.string.card_number,
        keyboardType = KeyboardType.Number
    )

    AddType.WIFI -> TypedSecretFieldSpec(
        labelRes = R.string.wifi_password,
        obscured = true
    )

    AddType.SSH_KEY -> TypedSecretFieldSpec(
        labelRes = R.string.ssh_private_key,
        singleLine = false
    )

    AddType.ID_CARD -> TypedSecretFieldSpec(
        labelRes = R.string.id_number
    )

    AddType.SEED_PHRASE -> TypedSecretFieldSpec(
        labelRes = R.string.seed_phrase,
        singleLine = false
    )

    AddType.PASSKEY -> TypedSecretFieldSpec(
        labelRes = R.string.passkey_data,
        singleLine = false
    )

    AddType.RECOVERY_CODE -> TypedSecretFieldSpec(
        labelRes = R.string.vault_fab_recovery_code,
        singleLine = false
    )

    else -> TypedSecretFieldSpec(
        labelRes = R.string.password,
        obscured = true
    )
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
