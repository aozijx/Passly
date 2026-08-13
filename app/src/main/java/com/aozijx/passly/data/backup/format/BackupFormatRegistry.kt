package com.aozijx.passly.data.backup.format

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.domain.backup.model.BackupFormatId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves external formats without coupling the service to concrete codecs.
 *
 * Adding an importer only requires a new adapter and one DI multibinding.
 */
@Singleton
internal class BackupFormatRegistry @Inject constructor(
    exporters: Set<@JvmSuppressWildcards BackupExportAdapter>,
    importers: Set<@JvmSuppressWildcards BackupImportAdapter>
) {
    private val exportersById = exporters.associateUnique("export")
    private val importersById = importers.associateUnique("import")
    private val importers = importers.sortedBy { it.formatId.value }

    fun exporter(formatId: BackupFormatId): BackupExportAdapter =
        exportersById[formatId] ?: throw BackupFailed()

    fun importer(
        requestedFormat: BackupFormatId?,
        payload: ByteArray
    ): BackupImportAdapter {
        if (requestedFormat != null) {
            val adapter = importersById[requestedFormat] ?: throw BackupFailed()
            if (adapter.probe(payload) <= 0) throw BackupFailed()
            return adapter
        }

        val candidates = importers
            .map { it to it.probe(payload) }
            .filter { (_, score) -> score > 0 }
        val bestScore = candidates.maxOfOrNull { it.second }
            ?: throw BackupFailed()
        val best = candidates.filter { it.second == bestScore }
        if (best.size != 1) {
            throw BackupFailed()
        }
        return best.single().first
    }

    private fun <T> Set<T>.associateUnique(
        kind: String
    ): Map<BackupFormatId, T> where T : Any {
        val result = linkedMapOf<BackupFormatId, T>()
        forEach { adapter ->
            val id = when (adapter) {
                is BackupExportAdapter -> adapter.formatId
                is BackupImportAdapter -> adapter.formatId
                else -> error("Unknown backup adapter")
            }
            require(result.put(id, adapter) == null) {
                "Duplicate backup $kind adapter: ${id.value}"
            }
        }
        return result
    }
}
