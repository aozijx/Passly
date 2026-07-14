package com.aozijx.passly.data.crypto

import com.aozijx.passly.data.model.payload.backup.AttachmentPayload
import com.aozijx.passly.data.model.serializer.toAttachmentPayload
import com.aozijx.passly.data.model.serializer.toJsonString
import com.aozijx.passly.security.crypto.FieldEncryptor

object AttachmentCipher {

    fun encrypt(
        payload: AttachmentPayload,
        entryId: String,
        attachmentId: String,
        fieldEncryptor: FieldEncryptor
    ): ByteArray = fieldEncryptor.encrypt(
        payload.toJsonString(),
        AadProvider.attachment(entryId, attachmentId)
    )

    fun decrypt(
        blob: ByteArray,
        entryId: String,
        attachmentId: String,
        fieldEncryptor: FieldEncryptor
    ): AttachmentPayload = fieldEncryptor.decrypt(
        blob,
        AadProvider.attachment(entryId, attachmentId)
    ).toAttachmentPayload()
}
