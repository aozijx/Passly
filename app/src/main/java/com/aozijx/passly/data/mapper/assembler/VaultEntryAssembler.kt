package com.aozijx.passly.data.mapper.assembler

import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.data.model.payload.snapshot.VaultSnapshot
import com.aozijx.passly.domain.model.entry.VaultCredential
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.VaultMetadata

object VaultEntryAssembler {

    fun assembleFromDatabase(
        entity: VaultMetadataEntity,
        meta: VaultMetadata,
        cred: VaultCredential?
    ): VaultEntry {
        return VaultEntry(
            metadata = meta.copy(
                entryId = entity.entryId,
                entryVersion = entity.entryVersion,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                deletedAt = entity.deletedAt
            ),
            credential = cred?.copy(entryId = entity.entryId)
                ?: VaultCredential(entryId = entity.entryId)
        )
    }

    fun assembleFromSnapshot(snapshot: VaultSnapshot): VaultEntry {
        return VaultEntry(
            metadata = snapshot.metadata.copy(
                entryId = snapshot.id,
                createdAt = snapshot.createdAt,
                updatedAt = snapshot.updatedAt,
                deletedAt = snapshot.deletedAt
            ),
            credential = snapshot.credential.copy(entryId = snapshot.id)
        )
    }

    fun assembleFromMetadataOnly(
        entity: VaultMetadataEntity,
        meta: VaultMetadata
    ): VaultEntry {
        return VaultEntry(
            metadata = meta.copy(
                entryId = entity.entryId,
                entryVersion = entity.entryVersion,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                deletedAt = entity.deletedAt
            ),
            credential = VaultCredential(entryId = entity.entryId)
        )
    }
}
