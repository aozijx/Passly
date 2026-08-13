package com.aozijx.passly.feature.backup.internal.archive.format

import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.model.BackupFormatId

/**
 * Encodes the canonical [BackupBundle] into one external format.
 *
 * Adapters contain no database, Android URI, or UI code.
 */
internal interface BackupExportAdapter {
    val formatId: BackupFormatId
    val includesAttachments: Boolean
    val supportsIcons: Boolean
    val requiresPassword: Boolean

    fun encode(bundle: BackupBundle, password: CharArray?): ByteArray
}

/**
 * Decodes one external format into the canonical [BackupBundle].
 *
 * [probe] must be cheap and side-effect free. A score of zero means no match;
 * larger scores indicate a more definitive signature.
 */
internal interface BackupImportAdapter {
    val formatId: BackupFormatId
    val requiresPassword: Boolean

    fun probe(payload: ByteArray): Int

    fun decode(payload: ByteArray, password: CharArray?): BackupBundle
}
