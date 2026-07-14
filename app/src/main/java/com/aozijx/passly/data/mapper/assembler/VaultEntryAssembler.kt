package com.aozijx.passly.data.mapper.assembler

import com.aozijx.passly.data.mapper.credential.mergeCredential
import com.aozijx.passly.data.mapper.metadata.mergeMetadata
import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.data.model.payload.credential.CredentialPayload
import com.aozijx.passly.data.model.payload.metadata.MetadataPayload
import com.aozijx.passly.data.model.payload.snapshot.VaultSnapshot
import com.aozijx.passly.domain.model.VaultEntry

object VaultEntryAssembler {

    fun assembleFromDatabase(
        entity: VaultMetadataEntity,
        metaPayload: MetadataPayload,
        credPayload: CredentialPayload?
    ): VaultEntry {
        var entry = VaultEntry(
            id = entity.entryId,
            vaultId = entity.vaultId,
            entryVersion = entity.entryVersion,
            revision = entity.revision,
            deletedAt = entity.deletedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
        entry = entry.mergeMetadata(metaPayload)
        return credPayload?.let { entry.mergeCredential(it) } ?: entry
    }

    fun assembleFromSnapshot(snapshot: VaultSnapshot): VaultEntry {
        var entry = VaultEntry(
            id = snapshot.id,
            vaultId = snapshot.vaultId,
            entryVersion = snapshot.revision,
            revision = snapshot.revision,
            createdAt = snapshot.createdAt,
            updatedAt = snapshot.updatedAt
        )
        entry = entry.mergeMetadata(snapshot.metadata)
        return entry.mergeCredential(snapshot.credential)
    }

    fun assembleFromMetadataOnly(
        entity: VaultMetadataEntity,
        metaPayload: MetadataPayload
    ): VaultEntry {
        return VaultEntry(
            id = entity.entryId,
            vaultId = entity.vaultId,
            entryVersion = entity.entryVersion,
            revision = entity.revision,
            deletedAt = entity.deletedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        ).mergeMetadata(metaPayload)
    }
}