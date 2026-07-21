package com.aozijx.passly.data.crypto

import com.aozijx.passly.data.model.payload.snapshot.VaultSnapshot
import com.aozijx.passly.data.model.serializer.AppJson
import com.aozijx.passly.domain.model.entry.VaultCredential
import com.aozijx.passly.domain.model.entry.VaultMetadata
import com.aozijx.passly.security.crypto.FieldEncryptor

object EntryCipher {

    fun encryptMetadata(
        meta: VaultMetadata,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): ByteArray = fieldEncryptor.encrypt(
        AppJson.encodeToString(VaultMetadata.serializer(), meta),
        AadProvider.metadata(entryId)
    )

    fun decryptMetadata(
        blob: ByteArray,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): VaultMetadata = AppJson.decodeFromString(
        VaultMetadata.serializer(),
        fieldEncryptor.decrypt(blob, AadProvider.metadata(entryId))
    )

    fun encryptCredential(
        cred: VaultCredential,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): ByteArray = fieldEncryptor.encrypt(
        AppJson.encodeToString(VaultCredential.serializer(), cred),
        AadProvider.credential(entryId)
    )

    fun decryptCredential(
        blob: ByteArray,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): VaultCredential = AppJson.decodeFromString(
        VaultCredential.serializer(),
        fieldEncryptor.decrypt(blob, AadProvider.credential(entryId))
    )

    fun encryptSnapshot(
        snapshot: VaultSnapshot,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): ByteArray = fieldEncryptor.encrypt(
        AppJson.encodeToString(VaultSnapshot.serializer(), snapshot),
        AadProvider.history(entryId)
    )

    fun decryptSnapshot(
        blob: ByteArray,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): VaultSnapshot = AppJson.decodeFromString(
        VaultSnapshot.serializer(),
        fieldEncryptor.decrypt(blob, AadProvider.history(entryId))
    )
}
