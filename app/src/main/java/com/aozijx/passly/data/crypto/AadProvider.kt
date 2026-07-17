package com.aozijx.passly.data.crypto

import com.aozijx.passly.data.local.database.DatabaseSchema

object AadProvider {

    fun metadata(entryId: String): ByteArray =
        "${DatabaseSchema.TABLE_METADATA}:${entryId}:metadataBlob".toByteArray(Charsets.UTF_8)

    fun credential(entryId: String): ByteArray =
        "${DatabaseSchema.TABLE_CREDENTIALS}:${entryId}:credentialBlob".toByteArray(Charsets.UTF_8)

    fun history(entryId: String): ByteArray =
        "${DatabaseSchema.TABLE_HISTORY}:${entryId}:snapshotBlob".toByteArray(Charsets.UTF_8)

    fun attachment(entryId: String, attachmentId: String): ByteArray =
        "${DatabaseSchema.TABLE_ATTACHMENT}:${entryId}:${attachmentId}:encryptedBlob".toByteArray(
            Charsets.UTF_8
        )
}