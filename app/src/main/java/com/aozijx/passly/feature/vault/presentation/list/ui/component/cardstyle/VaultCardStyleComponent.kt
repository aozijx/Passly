package com.aozijx.passly.feature.vault.presentation.list.ui.component.cardstyle

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.otp.presentation.OtpUiState
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemUiModel

/**
 * One selectable appearance for a vault list card.
 *
 * Implement the component in its own file, then add the object to
 * [CardStyleRegistry]. No central render or preview branch is required.
 */
interface VaultCardStyleComponent {
    /** Stable value persisted in EntryCardPresentation.variantKey. */
    val key: String

    @get:StringRes
    val titleRes: Int

    /** Whether this style is a meaningful settings choice for [entryType]. */
    fun supportsEntryType(entryType: EntryType): Boolean = true

    /** Whether this appearance can render the capabilities of [entry]. */
    fun supports(entry: VaultListItemUiModel): Boolean = true

    @Composable
    fun Render(
        entry: VaultListItemUiModel,
        totpState: OtpUiState,
        onClick: () -> Unit,
    )

    @Composable
    fun Preview(
        entryType: EntryType,
        onClick: () -> Unit,
    )
}
