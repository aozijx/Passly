package com.aozijx.passly.feature.vault.components.editor

import androidx.compose.ui.text.input.KeyboardType
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.presentation.labelRes

data class EntryEditorSchema(
    val addType: AddType,
    val entryType: EntryType,
    val titleRes: Int,
    val sections: List<EntryEditorSectionSchema>
)

data class EntryEditorSectionSchema(
    val titleRes: Int,
    val fields: List<EntryEditorFieldSchema>
)

data class EntryEditorFieldSchema(
    val key: EntryEditorFieldKey,
    val labelRes: Int,
    val kind: EntryEditorFieldKind = EntryEditorFieldKind.Text,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val singleLine: Boolean = true,
    val required: Boolean = false
)

enum class EntryEditorFieldKey {
    TITLE,
    SUMMARY,
    TAGS,
    SECRET,
    NOTES
}

enum class EntryEditorFieldKind {
    Text,
    Password,
    Notes
}

fun AddType.toEntryEditorSchema(): EntryEditorSchema {
    val entryType = toEntryType()
    return EntryEditorSchema(
        addType = this,
        entryType = entryType,
        titleRes = labelRes,
        sections = listOf(
            EntryEditorSectionSchema(
                titleRes = R.string.vault_editor_section_basic_info,
                fields = listOf(
                    EntryEditorFieldSchema(
                        key = EntryEditorFieldKey.TITLE,
                        labelRes = R.string.title,
                        required = true
                    ),
                    EntryEditorFieldSchema(
                        key = EntryEditorFieldKey.SUMMARY,
                        labelRes = summaryFieldLabelRes()
                    ),
                    EntryEditorFieldSchema(
                        key = EntryEditorFieldKey.TAGS,
                        labelRes = R.string.entry_category
                    )
                )
            ),
            EntryEditorSectionSchema(
                titleRes = labelRes,
                fields = listOf(secretFieldSchema())
            ),
            EntryEditorSectionSchema(
                titleRes = R.string.vault_editor_section_details,
                fields = listOf(
                    EntryEditorFieldSchema(
                        key = EntryEditorFieldKey.NOTES,
                        labelRes = R.string.remark,
                        kind = EntryEditorFieldKind.Notes,
                        singleLine = false
                    )
                )
            )
        )
    )
}

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

private fun AddType.summaryFieldLabelRes(): Int = when (this) {
    AddType.BANK_CARD -> R.string.cardholder
    AddType.WIFI -> R.string.wifi_ssid
    else -> R.string.username_hint
}

private fun AddType.secretFieldSchema(): EntryEditorFieldSchema = when (this) {
    AddType.BANK_CARD -> EntryEditorFieldSchema(
        key = EntryEditorFieldKey.SECRET,
        labelRes = R.string.card_number,
        keyboardType = KeyboardType.Number
    )

    AddType.WIFI -> EntryEditorFieldSchema(
        key = EntryEditorFieldKey.SECRET,
        labelRes = R.string.wifi_password,
        kind = EntryEditorFieldKind.Password
    )

    AddType.SSH_KEY -> EntryEditorFieldSchema(
        key = EntryEditorFieldKey.SECRET,
        labelRes = R.string.ssh_private_key,
        singleLine = false
    )

    AddType.ID_CARD -> EntryEditorFieldSchema(
        key = EntryEditorFieldKey.SECRET,
        labelRes = R.string.id_number
    )

    AddType.SEED_PHRASE -> EntryEditorFieldSchema(
        key = EntryEditorFieldKey.SECRET,
        labelRes = R.string.seed_phrase,
        singleLine = false
    )

    AddType.PASSKEY -> EntryEditorFieldSchema(
        key = EntryEditorFieldKey.SECRET,
        labelRes = R.string.passkey_data,
        singleLine = false
    )

    AddType.RECOVERY_CODE -> EntryEditorFieldSchema(
        key = EntryEditorFieldKey.SECRET,
        labelRes = R.string.vault_fab_recovery_code,
        singleLine = false
    )

    else -> EntryEditorFieldSchema(
        key = EntryEditorFieldKey.SECRET,
        labelRes = R.string.password,
        kind = EntryEditorFieldKind.Password
    )
}
