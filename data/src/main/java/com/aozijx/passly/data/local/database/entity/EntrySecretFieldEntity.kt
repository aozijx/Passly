package com.aozijx.passly.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Single encrypted secret payload row for an entry.
 *
 * Replaces the legacy `entry_secrets.secretBlob` aggregate and `entry_sensitive_fields` rows:
 * sensitive values (`PASSWORD`, card CVV, …) live in their own field-keyed rows, while the
 * low-sensitivity structure (`SecretFieldKeys.STRUCT_BUNDLE`) is one aggregated encrypted row.
 * Readers address a specific ciphertext with `fieldKey` instead of decrypting a whole blob.
 */
@Entity(
    tableName = "entry_secret_fields",
    primaryKeys = ["entryId", "fieldKey"],
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["entryId"])]
)
data class EntrySecretFieldEntity(
    val entryId: String,
    val fieldKey: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val valueCipher: ByteArray,
    val keyVersion: Int,
    val updatedAt: Long
)
