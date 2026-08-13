package com.aozijx.passly.data.codec

import com.aozijx.passly.data.local.database.DatabaseSchema

internal object DatabaseRecordAad {

    fun entrySummary(entryId: String): ByteArray =
        "${DatabaseSchema.TABLE_ENTRIES}:${entryId}:summaryBlob".toByteArray(Charsets.UTF_8)

    fun entrySecret(entryId: String): ByteArray =
        "${DatabaseSchema.TABLE_SECRETS}:${entryId}:secretBlob".toByteArray(Charsets.UTF_8)

    fun sensitiveField(entryId: String, fieldKey: String): ByteArray =
        "${DatabaseSchema.TABLE_SENSITIVE_FIELDS}:${entryId}:${fieldKey}:valueCipher"
            .toByteArray(Charsets.UTF_8)

    fun revision(entryId: String): ByteArray =
        "${DatabaseSchema.TABLE_REVISIONS}:${entryId}:entryContentCipher".toByteArray(Charsets.UTF_8)

}
