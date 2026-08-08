package com.aozijx.passly.data.backup.format.json

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.data.backup.format.BackupExportAdapter
import com.aozijx.passly.data.backup.format.BackupImportAdapter
import com.aozijx.passly.data.backup.format.containsAscii
import com.aozijx.passly.data.backup.model.BackupBundle
import com.aozijx.passly.domain.backup.model.BackupFormatId
import com.aozijx.passly.domain.backup.model.BackupFormats
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PasslyJsonFormatAdapter @Inject constructor(
    private val exporter: JsonBackupExporter,
    private val importer: JsonBackupImporter
) : BackupExportAdapter, BackupImportAdapter {
    override val formatId: BackupFormatId = BackupFormats.PASSLY_JSON
    override val includesAttachments: Boolean = true
    override val supportsIcons: Boolean = true
    override val requiresPassword: Boolean = false

    override fun encode(bundle: BackupBundle, password: CharArray?): ByteArray =
        exporter.export(bundle).toByteArray(Charsets.UTF_8)

    override fun probe(payload: ByteArray): Int =
        if (
            payload.containsAscii("\"document\"") &&
            payload.containsAscii("\"format\"") &&
            payload.containsAscii("\"passly-vault\"")
        ) 90 else 0

    override fun decode(payload: ByteArray, password: CharArray?): BackupBundle =
        try {
            importer.import(payload)
        } catch (error: BackupFailed) {
            throw error
        } catch (_: Exception) {
            throw BackupFailed()
        }
}
