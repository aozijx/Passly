package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.EntrySummary
import com.aozijx.passly.domain.model.lookup.EntryListItem
import com.aozijx.passly.domain.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.model.otp.OtpType

object EntryListItemMapper {

    fun assemble(
        entity: EntryEntity,
        summary: EntrySummary,
        secret: EntrySecret?
    ): EntryListItem {
        val hasTotp: Boolean
        val totpPeriod: Int
        val totpDigits: Int
        val otpType: OtpType
        val totpAlgorithm: OtpHashAlgorithm
        if (secret != null) {
            val otpConfig = when (secret) {
                is EntrySecret.Otp -> secret.data.config
                else -> null
            }
            if (otpConfig != null && otpConfig.secret.isNotBlank()) {
                hasTotp = true
                totpPeriod = otpConfig.periodSeconds ?: 30
                totpDigits = otpConfig.digits
                otpType = otpConfig.type
                totpAlgorithm = otpConfig.algorithm
            } else {
                hasTotp = false
                totpPeriod = 30
                totpDigits = 6
                otpType = OtpType.TOTP
                totpAlgorithm = OtpHashAlgorithm.SHA1
            }
        } else {
            hasTotp = false
            totpPeriod = 30
            totpDigits = 6
            otpType = OtpType.TOTP
            totpAlgorithm = OtpHashAlgorithm.SHA1
        }

        return EntryListItem(
            id = entity.entryId,
            entryType = entity.entryType,
            title = summary.title,
            username = summary.username,
            icon = summary.icon,
            iconCustomPath = summary.iconCustomPath,
            website = summary.website,
            favorite = summary.favorite,
            tags = summary.tags,
            color = summary.color,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            deletedAt = entity.deletedAt,
            expiresAt = summary.expiresAt,
            lastUsedAt = null,
            usageCount = 0,
            entryVersion = entity.version,
            hasTotp = hasTotp,
            totpPeriod = totpPeriod,
            totpDigits = totpDigits,
            otpType = otpType,
            totpAlgorithm = totpAlgorithm
        )
    }
}
