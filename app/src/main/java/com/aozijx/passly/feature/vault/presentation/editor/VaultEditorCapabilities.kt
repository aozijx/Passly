package com.aozijx.passly.feature.vault.presentation.editor

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.model.AddType

/** Editor flows currently owned by the Vault feature. Navigation resolves their destinations separately. */
internal object VaultEditorCapabilities {
    val creatableTypes: List<AddType> =
        listOf(
            AddType.PASSWORD,
            AddType.TOTP,
            AddType.BANK_CARD,
            AddType.WIFI,
        )

    private val fullFormEditableTypes =
        setOf(
            EntryType.LOGIN,
            EntryType.OTP,
            EntryType.BANK_CARD,
            EntryType.WIFI,
        )

    fun supportsFullFormEdit(type: EntryType): Boolean = type in fullFormEditableTypes
}
