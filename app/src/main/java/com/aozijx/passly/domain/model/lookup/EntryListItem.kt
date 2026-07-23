package com.aozijx.passly.domain.model.lookup

import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultIconable
import com.aozijx.passly.domain.model.entry.WebsiteInfo
import com.aozijx.passly.domain.model.otp.OtpHashAlgorithm
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
    val hasTotp: Boolean,
    val totpPeriod: Int = 30,
    val totpDigits: Int = 6,
    val otpType: OtpType = OtpType.TOTP,
    val totpAlgorithm: OtpHashAlgorithm = OtpHashAlgorithm.SHA1
) : VaultIconable {
    override val category: String get() = entryType.name
    override val iconName: String? get() = icon
    override val associatedAppPackage: String? get() = website?.packageNames?.firstOrNull()
    override val associatedDomain: String? get() = website?.primaryUrl
}
