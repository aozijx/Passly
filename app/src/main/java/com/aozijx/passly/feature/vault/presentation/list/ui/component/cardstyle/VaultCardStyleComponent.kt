package com.aozijx.passly.feature.vault.presentation.list.ui.component.cardstyle

import androidx.compose.runtime.Composable
import androidx.annotation.StringRes
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListItemUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultOtpKindUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultOtpUiState

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
        totpState: VaultOtpUiState?,
        showTotpCode: Boolean,
        onClick: () -> Unit,
    )

    @Composable
    fun Preview(entryType: EntryType, onClick: () -> Unit)
}

internal object CardStylePreviewFixtures {
    fun defaultEntry(entryType: EntryType) =
        previewEntry(
            entryType = entryType,
            id = "-100",
            title = "示例账号",
            username = "demo_user",
            associatedDomain = "example.com",
            hasOtp = entryType == EntryType.OTP,
            otpKind = VaultOtpKindUiModel.STANDARD.takeIf { entryType == EntryType.OTP },
        )

    fun passwordEntry(entryType: EntryType) =
        previewEntry(
            entryType = entryType,
            id = "-101",
            title = "我的邮箱",
            username = "me@example.com",
            associatedDomain = "example.com",
            hasPassword = true,
        )

    fun totpEntry(entryType: EntryType) =
        previewEntry(
            entryType = entryType,
            id = "-102",
            title = "示例二步验证",
            username = "totp_user",
            hasOtp = true,
            otpKind = VaultOtpKindUiModel.STANDARD,
        )

    private fun previewEntry(
        entryType: EntryType,
        id: String,
        title: String,
        username: String,
        associatedDomain: String? = null,
        hasPassword: Boolean = false,
        hasOtp: Boolean = false,
        otpKind: VaultOtpKindUiModel? = null,
    ) = VaultListItemUiModel(
        id = id,
        entryType = entryType,
        title = title,
        username = username,
        category = null,
        favorite = false,
        associatedDomain = associatedDomain,
        associatedAppPackage = null,
        iconName = null,
        iconCustomPath = null,
        hasPassword = hasPassword,
        hasOtp = hasOtp,
        otpKind = otpKind,
        otpPreview = null,
    )
}
