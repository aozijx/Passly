package com.aozijx.passly.presentation.ui.vault.list.component.cardstyle

import com.aozijx.passly.presentation.ui.vault.list.model.VaultCardPresentationUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel

object CardStyleRegistry {
    /*
     * Extension point: a new card appearance only needs one registration here.
     * The component owns its key, capability check, real content and preview.
     */
    private val registrations = listOf(
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

    /** Registered appearances in the same order used by a future style picker. */
    val styles: List<VaultCardStyleComponent>
        get() = registrations

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
        val presentation = presentations.firstOrNull {
            it.entryTypeKey.equals(entry.entryType.name, ignoreCase = true)
        }
        val requested = registrationsByKey[presentation?.variantKey.normalizedStyleKey()]
            ?: defaultStyle
        return requested.takeIf { it.supports(entry) } ?: defaultStyle
    }

    private fun String?.normalizedStyleKey(): String = this?.trim()?.lowercase().orEmpty()
}
