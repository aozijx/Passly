package com.aozijx.passly.feature.backup.internal.archive.format.text

import com.aozijx.passly.feature.backup.internal.archive.format.BackupExportAdapter
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.model.BackupFormatId
import com.aozijx.passly.feature.backup.internal.model.BackupFormats
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ReadableTextFormatAdapter
    @Inject
    constructor(
        private val exporter: TextVaultExporter,
    ) : BackupExportAdapter {
        override val formatId: BackupFormatId = BackupFormats.READABLE_TEXT
        override val includesAttachments: Boolean = false
        override val supportsIcons: Boolean = false
        override val requiresPassword: Boolean = false

        override fun encode(
            bundle: BackupBundle,
            password: CharArray?,
            output: OutputStream,
        ) {
            output.writer(Charsets.UTF_8).use { writer ->
                exporter.export(bundle.document.entries, TextExportOptions(), writer)
            }
        }
    }
