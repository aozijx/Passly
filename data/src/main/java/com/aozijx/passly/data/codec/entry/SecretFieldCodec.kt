package com.aozijx.passly.data.codec.entry

import com.aozijx.passly.data.codec.DatabaseRecordAad
import com.aozijx.passly.data.codec.toStorageKey
import com.aozijx.passly.core.crypto.SensitiveFieldCryptor
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import javax.inject.Inject

/**
 * Encrypts and decrypts one field-level secret payload. Each field gets its own AES-GCM
 * ciphertext bound to `entryId + fieldKey`, so revealing one field never decrypts the others.
 */
class SecretFieldCodec
    @Inject
    constructor(
        private val fieldEncryptor: SensitiveFieldCryptor,
    ) {
        suspend fun encrypt(
            entryId: String,
            key: SensitiveFieldKey,
            value: String,
        ): ByteArray = fieldEncryptor.encrypt(value, DatabaseRecordAad.secretField(entryId, key.toStorageKey()))

        suspend fun decrypt(
            entryId: String,
            key: SensitiveFieldKey,
            cipher: ByteArray,
        ): String = fieldEncryptor.decrypt(cipher, DatabaseRecordAad.secretField(entryId, key.toStorageKey()))

    }
