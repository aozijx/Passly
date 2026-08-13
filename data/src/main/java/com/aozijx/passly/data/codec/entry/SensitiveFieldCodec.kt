package com.aozijx.passly.data.codec.entry

import com.aozijx.passly.data.codec.DatabaseRecordAad
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.security.crypto.SensitiveFieldEncryptor
import javax.inject.Inject

class SensitiveFieldCodec @Inject constructor(
    private val fieldEncryptor: SensitiveFieldEncryptor
) {
    suspend fun encrypt(entryId: String, key: SensitiveFieldKey, value: String): ByteArray =
        fieldEncryptor.encrypt(value, DatabaseRecordAad.sensitiveField(entryId, key.name))

    suspend fun decrypt(entryId: String, key: SensitiveFieldKey, cipher: ByteArray): String =
        fieldEncryptor.decrypt(cipher, DatabaseRecordAad.sensitiveField(entryId, key.name))

    suspend fun decryptProvisioned(
        entryId: String,
        key: SensitiveFieldKey,
        cipher: ByteArray
    ): String = fieldEncryptor.decryptProvisioned(
        cipher,
        DatabaseRecordAad.sensitiveField(entryId, key.name)
    )
}
