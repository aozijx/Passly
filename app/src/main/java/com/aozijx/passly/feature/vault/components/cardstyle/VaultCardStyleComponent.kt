package com.aozijx.passly.feature.vault.components.cardstyle

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.EntryCapabilityFlags
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.OtpUiState
import com.aozijx.passly.domain.entry.model.WebsiteInfo
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem

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
    fun supports(entry: EntryListItem): Boolean = true

    @Composable
    fun Render(
        entry: EntryListItem,
        totpState: OtpUiState?,
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
        website = WebsiteInfo(primaryUrl = "example.com"),
    )

    val passwordEntry = previewEntry(
        id = "-101",
        title = "我的邮箱",
        username = "me@example.com",
        website = WebsiteInfo(primaryUrl = "example.com"),
        capabilityFlags = EntryCapabilityFlags.HAS_PASSWORD,
    )

    val totpEntry = previewEntry(
        id = "-102",
        title = "示例二步验证",
        username = "totp_user",
        capabilityFlags = EntryCapabilityFlags.HAS_OTP,
        otpTypeName = "TOTP",
    )

    private fun previewEntry(
        id: String,
        title: String,
        username: String,
        website: WebsiteInfo? = null,
        capabilityFlags: Int = 0,
        otpTypeName: String = "",
    ) = EntryListItem(
        id = id,
        entryType = EntryType.LOGIN,
        title = title,
        username = username,
        icon = null,
        iconCustomPath = null,
        website = website,
        favorite = false,
        tags = emptyList(),
        color = null,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
        expiresAt = null,
        lastUsedAt = null,
        usageCount = 0,
        entryVersion = 0,
        capabilityFlags = capabilityFlags,
        otpTypeName = otpTypeName,
    )
}
