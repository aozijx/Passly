package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

import com.aozijx.passly.feature.backup.internal.archive.format.BackupExportAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.BackupImportAdapter
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.model.BackupFormatId
import com.aozijx.passly.feature.backup.internal.model.BackupFormats
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PasslyEncryptedFormatAdapter @Inject constructor(
    private val exporter: EncryptedBackupExporter,
    private val importer: EncryptedBackupImporter
) : BackupExportAdapter, BackupImportAdapter {
    override val formatId: BackupFormatId = BackupFormats.PASSLY_ENCRYPTED
    override val includesAttachments: Boolean = true
    override val supportsIcons: Boolean = true
    override val requiresPassword: Boolean = true

    override fun encode(bundle: BackupBundle, password: CharArray?): ByteArray =
        exporter.export(bundle, requireNotNull(password) { "加密备份需要密码" })

    override fun probe(payload: ByteArray): Int =
        if (EncryptedBackupContainerCodec.hasMagic(payload)) 100 else 0

    override fun decode(payload: ByteArray, password: CharArray?): BackupBundle =
        importer.import(payload, requireNotNull(password) { "加密备份需要密码" })
}
