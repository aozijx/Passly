package com.aozijx.passly.feature.vault.presentation.list.ui.component.cardstyle

import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultCardPresentationUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListItemUiModel
import com.aozijx.passly.domain.entry.model.EntryType

object CardStyleRegistry {
    /*
     * Extension point: a new card appearance only needs one registration here.
     * The component owns its key, capability check, real content and preview.
     */
    private val registrations =
        listOf(
            DefaultVaultCardStyle,
            PasswordVaultCardStyle,
            TotpVaultCardStyle,
        )

    private val defaultStyle = registrations.first()
    private val registrationsByKey = registrations.associateBy { it.key.normalizedStyleKey() }

    init {
        require(registrations.isNotEmpty()) { "At least one vault card style must be registered" }
        require(registrationsByKey.size == registrations.size) {
            "Vault card style keys must be unique (case-insensitive)"
        }
        require(defaultStyle === DefaultVaultCardStyle) {
            "The default vault card style must be the first registration"
        }
    }

    /** Registered appearances in the order shown by settings. */
    val styles: List<VaultCardStyleComponent>
        get() = registrations

    val configurableEntryTypes: List<EntryType>
        get() = EntryType.entries.filter { stylesFor(it).size > 1 }

    fun stylesFor(entryType: EntryType): List<VaultCardStyleComponent> =
        registrations.filter { it.supportsEntryType(entryType) }

    fun entryTypeKey(entryType: EntryType): String =
        when (entryType) {
            EntryType.ACCOUNT -> "account"
            EntryType.LOGIN -> "login"
            EntryType.NOTE -> "note"
            EntryType.BANK_CARD -> "bank_card"
            EntryType.ID_CARD -> "id_card"
            EntryType.PASSPORT -> "passport"
            EntryType.DRIVER_LICENSE -> "driver_license"
            EntryType.SSH_KEY -> "ssh_key"
            EntryType.WIFI -> "wifi"
            EntryType.PASSKEY -> "passkey"
            EntryType.OTP -> "otp"
            EntryType.DATABASE_CREDENTIAL -> "database_credential"
            EntryType.SERVER_CREDENTIAL -> "server_credential"
            EntryType.API_KEY -> "api_key"
            EntryType.CRYPTO_WALLET -> "crypto_wallet"
            EntryType.SEED_PHRASE -> "seed_phrase"
            EntryType.RECOVERY_CODE -> "recovery_code"
        }

    /**
     * Resolves one primary list-card style for an entry.
     *
     * Detail-page capability composition is intentionally handled elsewhere.
     * A mixed login + OTP entry still occupies one list row and uses its
     * primary entry type's presentation.
     */
    fun resolveStyle(
        entry: VaultListItemUiModel,
        presentations: List<VaultCardPresentationUiModel>,
    ): VaultCardStyleComponent {
        val presentation =
            presentations.firstOrNull {
                it.entryTypeKey == entryTypeKey(entry.entryType)
            }
        val requested =
            registrationsByKey[presentation?.variantKey.normalizedStyleKey()]
                ?: defaultStyle
        return requested.takeIf {
            it.supportsEntryType(entry.entryType) && it.supports(entry)
        } ?: defaultStyle
    }

    private fun String?.normalizedStyleKey(): String = this?.trim()?.lowercase().orEmpty()
}
