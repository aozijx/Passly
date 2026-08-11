package com.aozijx.passly.data.codec.entry

import com.aozijx.passly.data.crypto.AadProvider
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.security.crypto.FieldEncryptor
import javax.inject.Inject

class SensitiveFieldCodec @Inject constructor(
    private val fieldEncryptor: FieldEncryptor
) {
    fun encrypt(entryId: String, key: SensitiveFieldKey, value: String): ByteArray =
        fieldEncryptor.encrypt(value, AadProvider.sensitiveField(entryId, key.name))

    fun decrypt(entryId: String, key: SensitiveFieldKey, cipher: ByteArray): String =
        fieldEncryptor.decrypt(cipher, AadProvider.sensitiveField(entryId, key.name))
}
