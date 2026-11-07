package com.aozijx.passly.feature.backup.internal.archive.format

import com.aozijx.passly.feature.backup.contract.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.model.BackupFormatId
import java.io.InputStream
import java.io.OutputStream

/** Shared identity of a format codec. */
internal interface BackupFormatAdapter {
    val formatId: BackupFormatId
}

/** Encodes the canonical [BackupBundle] into one external format. */
internal interface BackupExportAdapter : BackupFormatAdapter {
    val includesAttachments: Boolean
    val supportsIcons: Boolean
    val requiresPassword: Boolean

    fun encode(
        bundle: BackupBundle,
        password: CharArray?,
        output: OutputStream,
    )
}

/** Decodes one external format into the canonical [BackupBundle]. */
internal interface BackupImportAdapter : BackupFormatAdapter {
    val requiresPassword: Boolean

    fun decode(
        input: InputStream,
        password: CharArray?,
        strategy: BackupImportStrategy = BackupImportStrategy.STRICT,
    ): BackupBundle
}
