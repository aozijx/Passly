package com.aozijx.passly.domain.model.lookup

import com.aozijx.passly.domain.model.core.OtpHashAlgorithm
import com.aozijx.passly.domain.model.core.OtpType
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultIconable
import com.aozijx.passly.domain.model.entry.WebsiteInfo

/**
 * 列表项 —— 仅包含列表 UI 所需的非敏感字段。
 *
 * 相比完整的 [VaultEntry][com.aozijx.passly.domain.model.entry.VaultEntry]，
 * VaultListItem 不包含密码、TOTP Secret、SSH 密钥等敏感凭据数据，
 * 防止完整凭据长期驻留在 StateFlow 中。
 */
data class VaultListItem(
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
    /** 是否包含 TOTP 配置（用于 Tab 筛选），不暴露 secret。 */
    val hasTotp: Boolean,
    /** TOTP 周期（秒），仅在有 TOTP 时有效。 */
    val totpPeriod: Int = 30,
    /** TOTP 位数，仅在有 TOTP 时有效。 */
    val totpDigits: Int = 6,
    /** OTP 类型（TOTP/HOTP/STEAM），仅在有 OTP 时有效。 */
    val otpType: OtpType = OtpType.TOTP,
    /** TOTP 哈希算法，仅在有 TOTP 时有效。 */
    val totpAlgorithm: OtpHashAlgorithm = OtpHashAlgorithm.SHA1
) : VaultIconable {
    override val category: String get() = entryType.name
    override val iconName: String? get() = icon
    override val associatedAppPackage: String? get() = website?.packageNames?.firstOrNull()
    override val associatedDomain: String? get() = website?.primaryUrl
}
