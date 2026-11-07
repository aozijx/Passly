package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

import com.aozijx.passly.feature.backup.contract.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.archive.format.BackupExportAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.BackupImportAdapter
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.model.BackupFormatId
import com.aozijx.passly.feature.backup.internal.model.BackupFormats
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PasslyEncryptedFormatAdapter
    @Inject
    constructor(
        private val exporter: EncryptedBackupExporter,
        private val importer: EncryptedBackupImporter,
    ) : BackupExportAdapter,
        BackupImportAdapter {
        override val formatId: BackupFormatId = BackupFormats.PASSLY_ENCRYPTED
        override val includesAttachments: Boolean = true
        override val supportsIcons: Boolean = true
        override val requiresPassword: Boolean = true

        override fun encode(
            bundle: BackupBundle,
            password: CharArray?,
            output: OutputStream,
        ) = exporter.export(bundle, requireNotNull(password) { "加密备份需要密码" }, output)

        override fun decode(
            input: InputStream,
            password: CharArray?,
            strategy: BackupImportStrategy,
        ): BackupBundle =
            importer.import(
                input,
                requireNotNull(password) { "加密备份需要密码" },
                strategy,
            )
    }
