package com.aozijx.passly.presentation.ui.vault.list.component.cardstyle

import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.ui.shared.entry.EntryTypeUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpKindUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpUiState

/**
 * One selectable appearance for a vault list card.
 *
 * Implement the component in its own file, then add the object to
 * [CardStyleRegistry]. No central render or preview branch is required.
 */
interface VaultCardStyleComponent {
    /** Stable value persisted in EntryCardPresentation.variantKey. */
    val key: String

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
    fun Preview(onClick: () -> Unit)
}

internal object CardStylePreviewFixtures {
    val defaultEntry = previewEntry(
        id = "-100",
        title = "示例账号",
        username = "demo_user",
        associatedDomain = "example.com",
    )

    val passwordEntry = previewEntry(
        id = "-101",
        title = "我的邮箱",
        username = "me@example.com",
        associatedDomain = "example.com",
        hasPassword = true,
    )

    val totpEntry = previewEntry(
        id = "-102",
        title = "示例二步验证",
        username = "totp_user",
        hasOtp = true,
        otpKind = VaultOtpKindUiModel.STANDARD,
    )

    private fun previewEntry(
        id: String,
        title: String,
        username: String,
        associatedDomain: String? = null,
        hasPassword: Boolean = false,
        hasOtp: Boolean = false,
        otpKind: VaultOtpKindUiModel? = null,
    ) = VaultListItemUiModel(
        id = id,
        entryType = EntryTypeUiModel.LOGIN,
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
