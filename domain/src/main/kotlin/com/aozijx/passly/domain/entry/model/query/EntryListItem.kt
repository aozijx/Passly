package com.aozijx.passly.domain.entry.model.query

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.otp.OtpType

data class EntryUsage(
    val lastUsedAtMs: Long? = null,
    val count: Int = 0,
) {
    init {
        require(lastUsedAtMs == null || lastUsedAtMs >= 0L) { "Last-used time cannot be negative" }
        require(count >= 0) { "Usage count cannot be negative" }
    }
}

/** Read model for library queries; it is intentionally not the Entry aggregate. */
data class EntryListItem(
    val identity: EntryIdentity,
    val profile: EntryProfile,
    val usage: EntryUsage = EntryUsage(),
    val capabilities: EntryCapabilities = EntryCapabilities(),
    val otpType: OtpType? = null,
    val accountId: EntryId? = null,
) {
    val id: EntryId get() = identity.id
    val entryType: EntryType get() = identity.type
    val title: String get() = profile.title
    val favorite: Boolean get() = profile.favorite
    val createdAt: Long get() = identity.timestamps.createdAtMs
    val updatedAt: Long get() = identity.timestamps.updatedAtMs
    val lastUsedAt: Long? get() = usage.lastUsedAtMs
    val usageCount: Int get() = usage.count
    val hasPassword: Boolean get() = EntryCapability.PASSWORD in capabilities
    val hasOtp: Boolean get() = EntryCapability.OTP in capabilities
    val hasAttachments: Boolean get() = EntryCapability.ATTACHMENTS in capabilities
}
