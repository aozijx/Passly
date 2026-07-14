package com.aozijx.passly.data.crypto

import com.aozijx.passly.data.local.database.DatabaseConfig

object AadProvider {

    fun metadata(entryId: String): ByteArray =
        "${DatabaseConfig.TABLE_METADATA}:${entryId}:metadataBlob".toByteArray(Charsets.UTF_8)

    fun credential(entryId: String): ByteArray =
        "${DatabaseConfig.TABLE_CREDENTIALS}:${entryId}:credentialBlob".toByteArray(Charsets.UTF_8)

    fun history(entryId: String): ByteArray =
        "${DatabaseConfig.TABLE_HISTORY}:${entryId}:snapshotBlob".toByteArray(Charsets.UTF_8)

    fun attachment(entryId: String, attachmentId: String): ByteArray =
        "${DatabaseConfig.TABLE_ATTACHMENTS}:${entryId}:${attachmentId}:encryptedBlob".toByteArray(
            Charsets.UTF_8
        )
}