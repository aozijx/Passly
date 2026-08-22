package com.aozijx.passly.data.codec

import com.aozijx.passly.data.local.database.DatabaseSchema

internal object DatabaseRecordAad {

    fun secretBundle(entryId: String): ByteArray =
        "${DatabaseSchema.TABLE_SECRET_FIELDS}:${entryId}:STRUCT_BUNDLE".toByteArray(Charsets.UTF_8)

    fun secretField(entryId: String, fieldKey: String): ByteArray =
        "${DatabaseSchema.TABLE_SECRET_FIELDS}:${entryId}:${fieldKey}:valueCipher"
            .toByteArray(Charsets.UTF_8)

    fun revision(entryId: String): ByteArray =
        "${DatabaseSchema.TABLE_REVISIONS}:${entryId}:entryContentCipher".toByteArray(Charsets.UTF_8)

}
