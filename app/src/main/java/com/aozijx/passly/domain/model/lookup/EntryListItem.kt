package com.aozijx.passly.domain.model.lookup

import com.aozijx.passly.domain.model.entry.EntryCapabilityFlags
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultIconable
import com.aozijx.passly.domain.model.entry.WebsiteInfo
import com.aozijx.passly.domain.model.otp.OtpType

data class EntryListItem(
    val id: String,
    val entryType: EntryType,
    val title: String,
    val username: String,
    val icon: String?,
    override val iconCustomPath: String?,
    val website: WebsiteInfo?,
    val favorite: Boolean,
    val tags: List<String>,
    val color: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val expiresAt: Long?,
    val lastUsedAt: Long?,
    val usageCount: Int,
    val entryVersion: Int,
    val capabilityFlags: Int,
    /** OTP 类型名（TOTP/HOTP/STEAM），仅 [hasOtp] 为 true 时有意义。 */
    val otpTypeName: String = ""
) : VaultIconable {
    val hasPassword: Boolean
        get() = EntryCapabilityFlags.has(capabilityFlags, EntryCapabilityFlags.HAS_PASSWORD)
    val hasOtp: Boolean
        get() = EntryCapabilityFlags.has(capabilityFlags, EntryCapabilityFlags.HAS_OTP)
    val hasAttachments: Boolean
        get() = EntryCapabilityFlags.has(capabilityFlags, EntryCapabilityFlags.HAS_ATTACHMENTS)
    val otpType: OtpType get() = runCatching { OtpType.valueOf(otpTypeName) }.getOrDefault(OtpType.TOTP)
    override val category: String get() = entryType.name
    override val iconName: String? get() = icon
    override val associatedAppPackage: String? get() = website?.packageNames?.firstOrNull()
    override val associatedDomain: String? get() = website?.primaryUrl
}
