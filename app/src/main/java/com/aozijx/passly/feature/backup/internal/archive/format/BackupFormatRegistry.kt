package com.aozijx.passly.feature.backup.internal.archive.format

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.feature.backup.internal.archive.format.encrypted.BackupContainerHeader
import com.aozijx.passly.feature.backup.internal.archive.format.encrypted.BackupContainerV1Spec
import com.aozijx.passly.feature.backup.internal.archive.format.json.JsonFormatDispatcher
import com.aozijx.passly.feature.backup.internal.model.BackupFormatId
import com.aozijx.passly.feature.backup.internal.model.BackupFormats
import java.io.InputStream
import java.io.PushbackInputStream
import javax.inject.Inject
import javax.inject.Singleton

internal data class BackupImportSelection(
    val adapter: BackupImportAdapter,
    val input: InputStream,
)

/** Resolves external formats without coupling the service to concrete codecs. */
@Singleton
internal class BackupFormatRegistry
    @Inject
    constructor(
        exporters: Set<@JvmSuppressWildcards BackupExportAdapter>,
        importers: Set<@JvmSuppressWildcards BackupImportAdapter>,
        private val jsonDispatcher: JsonFormatDispatcher,
    ) {
        private val exportersById = exporters.associateUnique("export")
        private val importersById = importers.associateUnique("import")

        fun exporter(formatId: BackupFormatId): BackupExportAdapter =
            exportersById[formatId] ?: throw BackupFailed(reasonCode = Reasons.Backup.ImportExport.BackupFormatUnsupported.value)

        fun selectImporter(
            requestedFormat: BackupFormatId?,
            source: InputStream,
        ): BackupImportSelection {
            if (requestedFormat != null) {
                return BackupImportSelection(
                    importersById[requestedFormat]
                        ?: throw BackupFailed(reasonCode = Reasons.Backup.ImportExport.BackupFormatUnsupported.value),
                    source,
                )
            }
            val input = PushbackInputStream(source, BackupContainerV1Spec.MAGIC_LENGTH)
            val prefix = ByteArray(BackupContainerV1Spec.MAGIC_LENGTH)
            var length = 0
            while (length < prefix.size) {
                val count = input.read(prefix, length, prefix.size - length)
                if (count < 0) break
                if (count == 0) {
                    val next = input.read()
                    if (next < 0) break
                    prefix[length++] = next.toByte()
                } else {
                    length += count
                }
            }
            if (length > 0) input.unread(prefix, 0, length)
            val adapter =
                if (BackupContainerHeader.hasMagic(prefix, length)) {
                    importersById[BackupFormats.PASSLY_ENCRYPTED]
                        ?: throw BackupFailed(reasonCode = Reasons.Backup.ImportExport.BackupFormatUnsupported.value)
                } else {
                    jsonDispatcher
                }
            return BackupImportSelection(adapter, input)
        }

        private fun <T : BackupFormatAdapter> Set<T>.associateUnique(kind: String): Map<BackupFormatId, T> {
            val result = linkedMapOf<BackupFormatId, T>()
            forEach { adapter ->
                require(result.put(adapter.formatId, adapter) == null) {
                    "Duplicate backup $kind adapter: ${adapter.formatId.value}"
                }
            }
            return result
        }
    }
