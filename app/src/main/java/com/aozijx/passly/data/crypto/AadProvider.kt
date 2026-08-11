package com.aozijx.passly.data.crypto

import com.aozijx.passly.data.local.database.DatabaseSchema

object AadProvider {

    fun metadata(entryId: String): ByteArray =
        "${DatabaseSchema.TABLE_ENTRIES}:${entryId}:summaryBlob".toByteArray(Charsets.UTF_8)

    fun credential(entryId: String): ByteArray =
        "${DatabaseSchema.TABLE_SECRETS}:${entryId}:secretBlob".toByteArray(Charsets.UTF_8)

    fun sensitiveField(entryId: String, fieldKey: String): ByteArray =
        "${DatabaseSchema.TABLE_SENSITIVE_FIELDS}:${entryId}:${fieldKey}:valueCipher"
            .toByteArray(Charsets.UTF_8)

    fun revision(entryId: String): ByteArray =
        "${DatabaseSchema.TABLE_REVISIONS}:${entryId}:entryBlob".toByteArray(Charsets.UTF_8)

    fun history(entryId: String): ByteArray =
        "${DatabaseSchema.TABLE_REVISIONS}:${entryId}:historyBlob".toByteArray(Charsets.UTF_8)

    fun attachment(entryId: String, attachmentId: String): ByteArray =
        "${DatabaseSchema.TABLE_ATTACHMENT}:${entryId}:${attachmentId}:encryptedBlob".toByteArray(
            Charsets.UTF_8
        )

    fun attachmentContent(entryId: String, attachmentId: String): ByteArray =
        "${DatabaseSchema.TABLE_ATTACHMENT}:${entryId}:${attachmentId}:content".toByteArray(
            Charsets.UTF_8
        )
}
