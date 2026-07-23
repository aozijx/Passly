package com.aozijx.passly.data.mapper.assembler

import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.data.model.payload.snapshot.VaultSnapshot
import com.aozijx.passly.domain.model.core.OtpHashAlgorithm
import com.aozijx.passly.domain.model.core.OtpType
import com.aozijx.passly.domain.model.entry.VaultCredential
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.VaultMetadata
import com.aozijx.passly.domain.model.lookup.VaultListItem

object VaultEntryAssembler {

    fun assembleFromDatabase(
        entity: VaultMetadataEntity,
        meta: VaultMetadata,
        cred: VaultCredential?
    ): VaultEntry {
        return VaultEntry(
            metadata = meta.copy(entryId = entity.entryId),
            credential = cred?.copy(entryId = entity.entryId)
                ?: VaultCredential(entryId = entity.entryId),
            entryVersion = entity.entryVersion,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            deletedAt = entity.deletedAt
        )
    }

    fun assembleFromSnapshot(snapshot: VaultSnapshot): VaultEntry {
        return VaultEntry(
            metadata = snapshot.metadata.copy(entryId = snapshot.id),
            credential = snapshot.credential.copy(entryId = snapshot.id),
            entryVersion = snapshot.revision,
            createdAt = snapshot.createdAt,
            updatedAt = snapshot.updatedAt,
            deletedAt = snapshot.deletedAt
        )
    }

    fun assembleListItem(
        entity: VaultMetadataEntity,
        meta: VaultMetadata,
        hasTotp: Boolean,
        totpPeriod: Int,
        totpDigits: Int,
        otpType: OtpType,
        totpAlgorithm: OtpHashAlgorithm
    ): VaultListItem {
        return VaultListItem(
            id = entity.entryId,
            entryType = meta.entryType,
            title = meta.title,
            username = meta.username,
            icon = meta.icon,
            iconCustomPath = meta.iconCustomPath,
            website = meta.website,
            favorite = meta.favorite,
            tags = meta.tags,
            color = meta.color,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            deletedAt = entity.deletedAt,
            expiresAt = meta.expiresAt,
            lastUsedAt = null,
            usageCount = 0,
            entryVersion = entity.entryVersion,
            hasTotp = hasTotp,
            totpPeriod = totpPeriod,
            totpDigits = totpDigits,
            otpType = otpType,
            totpAlgorithm = totpAlgorithm
        )
    }
}
