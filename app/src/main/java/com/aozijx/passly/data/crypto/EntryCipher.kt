package com.aozijx.passly.data.crypto

import com.aozijx.passly.data.model.payload.credential.CredentialPayload
import com.aozijx.passly.data.model.payload.metadata.MetadataPayload
import com.aozijx.passly.data.model.payload.snapshot.VaultSnapshot
import com.aozijx.passly.data.model.serializer.toCredentialPayload
import com.aozijx.passly.data.model.serializer.toJsonString
import com.aozijx.passly.data.model.serializer.toMetadataPayload
import com.aozijx.passly.data.model.serializer.toVaultSnapshot
import com.aozijx.passly.security.crypto.FieldEncryptor

object EntryCipher {

    fun encryptMetadata(
        payload: MetadataPayload,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): ByteArray = fieldEncryptor.encrypt(
        payload.toJsonString(),
        AadProvider.metadata(entryId)
    )

    fun decryptMetadata(
        blob: ByteArray,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): MetadataPayload = fieldEncryptor.decrypt(
        blob,
        AadProvider.metadata(entryId)
    ).toMetadataPayload()

    fun encryptCredential(
        payload: CredentialPayload,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): ByteArray = fieldEncryptor.encrypt(
        payload.toJsonString(),
        AadProvider.credential(entryId)
    )

    fun decryptCredential(
        blob: ByteArray,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): CredentialPayload = fieldEncryptor.decrypt(
        blob,
        AadProvider.credential(entryId)
    ).toCredentialPayload()

    fun encryptSnapshot(
        payload: VaultSnapshot,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): ByteArray = fieldEncryptor.encrypt(
        payload.toJsonString(),
        AadProvider.history(entryId)
    )

    fun decryptSnapshot(
        blob: ByteArray,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): VaultSnapshot = fieldEncryptor.decrypt(
        blob,
        AadProvider.history(entryId)
    ).toVaultSnapshot()
}
