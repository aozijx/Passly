package com.aozijx.passly.feature.backup.internal.archive.format.json

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.feature.backup.contract.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.archive.BackupIntegrityException
import com.aozijx.passly.feature.backup.internal.archive.format.BackupExportAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.BackupImportAdapter
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.model.BackupFormatId
import com.aozijx.passly.feature.backup.internal.model.BackupFormats
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
internal class PasslyJsonFormatAdapter
    @Inject
    constructor(
        private val exporter: JsonBackupExporter,
        private val importer: JsonBackupImporter,
    ) : BackupExportAdapter,
        BackupImportAdapter {
        override val formatId: BackupFormatId = BackupFormats.PASSLY_JSON
        override val includesAttachments: Boolean = true
        override val supportsIcons: Boolean = true
        override val requiresPassword: Boolean = false

        override fun encode(
            bundle: BackupBundle,
            password: CharArray?,
            output: OutputStream,
        ) {
            exporter.export(bundle, output)
        }

        override fun decode(
            input: InputStream,
            password: CharArray?,
            strategy: BackupImportStrategy,
        ): BackupBundle =
            try {
                importer.import(input, strategy)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: BackupFailed) {
                throw error
            } catch (error: BackupIntegrityException) {
                throw BackupFailed(reasonCode = error.reasonCode.value)
            } catch (error: Exception) {
                throw BackupFailed(
                    throwableType = error.javaClass.simpleName,
                    reasonCode = Reasons.Backup.Document.BackupDocumentMalformed.value,
                )
            }
    }
