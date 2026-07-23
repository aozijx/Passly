package com.aozijx.passly.data.crypto

import com.aozijx.passly.data.model.payload.secret.SecretPayload
import com.aozijx.passly.data.model.payload.snapshot.VaultSnapshot
import com.aozijx.passly.data.model.payload.summary.SummaryPayload
import com.aozijx.passly.data.model.serializer.AppJson
import com.aozijx.passly.security.crypto.FieldEncryptor

object EntryCipher {

    fun encryptSummary(
        summary: SummaryPayload,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): ByteArray = fieldEncryptor.encrypt(
        AppJson.encodeToString(SummaryPayload.serializer(), summary),
        AadProvider.metadata(entryId)
    )

    fun decryptSummary(
        blob: ByteArray,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): SummaryPayload = AppJson.decodeFromString(
        SummaryPayload.serializer(),
        fieldEncryptor.decrypt(blob, AadProvider.metadata(entryId))
    )

    fun encryptSecret(
        secret: SecretPayload,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): ByteArray = fieldEncryptor.encrypt(
        AppJson.encodeToString(SecretPayload.serializer(), secret),
        AadProvider.credential(entryId)
    )

    fun decryptSecret(
        blob: ByteArray,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): SecretPayload = AppJson.decodeFromString(
        SecretPayload.serializer(),
        fieldEncryptor.decrypt(blob, AadProvider.credential(entryId))
    )

    fun encryptSnapshot(
        snapshot: VaultSnapshot,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): ByteArray = fieldEncryptor.encrypt(
        AppJson.encodeToString(VaultSnapshot.serializer(), snapshot),
        AadProvider.revision(entryId)
    )

    fun decryptSnapshot(
        blob: ByteArray,
        entryId: String,
        fieldEncryptor: FieldEncryptor
    ): VaultSnapshot = AppJson.decodeFromString(
        VaultSnapshot.serializer(),
        fieldEncryptor.decrypt(blob, AadProvider.revision(entryId))
    )
}
