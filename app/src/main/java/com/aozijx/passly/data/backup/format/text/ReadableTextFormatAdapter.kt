package com.aozijx.passly.data.backup.format.text

import com.aozijx.passly.data.backup.format.BackupExportAdapter
import com.aozijx.passly.data.backup.model.BackupBundle
import com.aozijx.passly.domain.model.backup.BackupFormatId
import com.aozijx.passly.domain.model.backup.BackupFormats
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ReadableTextFormatAdapter @Inject constructor(
    private val exporter: TextVaultExporter
) : BackupExportAdapter {
    override val formatId: BackupFormatId = BackupFormats.READABLE_TEXT
    override val includesAttachments: Boolean = false
    override val supportsIcons: Boolean = false
    override val requiresPassword: Boolean = false

    override fun encode(bundle: BackupBundle, password: CharArray?): ByteArray =
        exporter.export(bundle.document.entries, TextExportOptions())
            .toByteArray(Charsets.UTF_8)
}
