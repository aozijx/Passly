package com.aozijx.passly.data.crypto

import com.aozijx.passly.data.model.payload.backup.AttachmentPayload
import com.aozijx.passly.data.model.serializer.AppJson
import com.aozijx.passly.security.crypto.FieldEncryptor

object AttachmentCipher {

    fun encrypt(
        payload: AttachmentPayload,
        entryId: String,
        attachmentId: String,
        fieldEncryptor: FieldEncryptor
    ): ByteArray = fieldEncryptor.encrypt(
        AppJson.encodeToString(AttachmentPayload.serializer(), payload),
        AadProvider.attachment(entryId, attachmentId)
    )

    fun decrypt(
        blob: ByteArray,
        entryId: String,
        attachmentId: String,
        fieldEncryptor: FieldEncryptor
    ): AttachmentPayload = AppJson.decodeFromString(
        AttachmentPayload.serializer(),
        fieldEncryptor.decrypt(
            blob,
            AadProvider.attachment(entryId, attachmentId)
        )
    )
}
