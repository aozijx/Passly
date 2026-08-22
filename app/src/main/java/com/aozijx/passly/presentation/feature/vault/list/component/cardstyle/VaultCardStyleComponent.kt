package com.aozijx.passly.presentation.feature.vault.list.component.cardstyle

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.query.EntryCapabilities
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.model.OtpUiState
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.query.EntryCapability
import com.aozijx.passly.domain.entry.model.query.EntryUsage
import com.aozijx.passly.domain.entry.model.otp.OtpType

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
        website = EntryAssociations(primaryUrl = "example.com"),
    )

    val passwordEntry = previewEntry(
        id = "-101",
        title = "我的邮箱",
        username = "me@example.com",
        website = EntryAssociations(primaryUrl = "example.com"),
        capabilities = EntryCapabilities(setOf(EntryCapability.PASSWORD)),
    )

    val totpEntry = previewEntry(
        id = "-102",
        title = "示例二步验证",
        username = "totp_user",
        capabilities = EntryCapabilities(setOf(EntryCapability.OTP)),
        otpType = OtpType.TOTP,
    )

    private fun previewEntry(
        id: String,
        title: String,
        username: String,
        website: EntryAssociations = EntryAssociations(),
        capabilities: EntryCapabilities = EntryCapabilities(),
        otpType: OtpType? = null,
    ) = EntryListItem(
        identity = EntryIdentity(
            id = EntryId(id),
            type = EntryType.LOGIN,
            timestamps = EntryTimestamps(0L),
        ),
        profile = EntryProfile(
            title = title,
            username = username,
            associations = website,
            icon = EntryIcon(),
        ),
        usage = EntryUsage(),
        capabilities = capabilities,
        otpType = otpType,
    )
}
