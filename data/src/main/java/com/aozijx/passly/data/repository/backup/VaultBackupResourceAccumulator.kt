package com.aozijx.passly.data.repository.backup

import com.aozijx.passly.domain.backup.model.VaultBackupReadOptions
import com.aozijx.passly.domain.backup.model.VaultBackupResource
import com.aozijx.passly.domain.backup.model.VaultBackupSnapshot
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.relation.EntryLink

/** Owns plaintext buffers until a complete snapshot can be returned to the caller. */
internal class VaultBackupResourceAccumulator(
    private val options: VaultBackupReadOptions,
) {
    private val resources = mutableListOf<VaultBackupResource>()

    var accumulatedBytes: Long = 0L
        private set

    fun add(resource: VaultBackupResource) {
        val resourceBytes = resource.content.size.toLong()
        if (resourceBytes > options.maxResourceBytes) {
            resource.content.fill(0)
            throw IllegalArgumentException("Vault backup resource exceeds its size limit")
        }
        if (resourceBytes > options.maxTotalResourceBytes - accumulatedBytes) {
            resource.content.fill(0)
            throw IllegalArgumentException("Vault backup resources exceed their cumulative size limit")
        }
        resources += resource
        accumulatedBytes += resourceBytes
    }

    fun toSnapshot(
        entries: List<Entry>,
        links: List<EntryLink>,
    ): VaultBackupSnapshot {
        val snapshot = VaultBackupSnapshot(entries, links, resources.toList())
        resources.clear()
        accumulatedBytes = 0L
        return snapshot
    }

    fun clear() {
        resources.forEach { it.content.fill(0) }
        resources.clear()
        accumulatedBytes = 0L
    }

    fun fail(failure: Throwable): Nothing {
        clear()
        throw failure
    }
}
