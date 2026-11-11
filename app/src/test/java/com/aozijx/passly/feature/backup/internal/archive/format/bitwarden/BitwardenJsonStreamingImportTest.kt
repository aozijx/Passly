package com.aozijx.passly.feature.backup.internal.archive.format.bitwarden

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.feature.backup.contract.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.archive.format.BackupExportAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.BackupFormatRegistry
import com.aozijx.passly.feature.backup.internal.archive.format.json.JsonBackupImporter
import com.aozijx.passly.feature.backup.internal.archive.format.json.JsonFormatDispatcher
import com.aozijx.passly.feature.backup.internal.model.BackupFormats
import java.io.InputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class BitwardenJsonStreamingImportTest {
    @Test
    fun unsupportedItemIsRejectedBeforeTheNextRootValueIsReadOnEveryJsonImportRoute() {
        listOf(null, BackupFormats.BITWARDEN_JSON).forEach { requestedFormat ->
            val input =
                BoundaryInputStream(
                    """{"items":[{"type":9,"name":"Unsupported"}],""".toByteArray(Charsets.UTF_8),
                )
            val bitwardenImporter = BitwardenJsonImportAdapter(BitwardenJsonParser())
            val registry =
                BackupFormatRegistry(
                    exporters = emptySet<BackupExportAdapter>(),
                    importers = setOf(bitwardenImporter),
                    jsonDispatcher = JsonFormatDispatcher(JsonBackupImporter(), bitwardenImporter),
                )
            val selection = registry.selectImporter(requestedFormat, input)

            assertThrows(BackupFailed::class.java) {
                selection.adapter.decode(
                    input = selection.input,
                    password = null,
                    strategy = BackupImportStrategy.STRICT,
                )
            }
            assertFalse("unsupported record must fail before parsing later root content", input.readPastBoundary)
        }
    }

    private class BoundaryInputStream(
        private val prefix: ByteArray,
    ) : InputStream() {
        private var position = 0

        var readPastBoundary = false
            private set

        override fun read(): Int {
            if (position == prefix.size) return failAtBoundary()
            return prefix[position++].toInt() and 0xff
        }

        override fun read(
            destination: ByteArray,
            offset: Int,
            length: Int,
        ): Int {
            if (length == 0) return 0
            if (position == prefix.size) return failAtBoundary()
            val count = minOf(length, prefix.size - position)
            prefix.copyInto(destination, offset, position, position + count)
            position += count
            return count
        }

        private fun failAtBoundary(): Nothing {
            readPastBoundary = true
            throw AssertionError("Parser read beyond the unsupported item")
        }
    }
}
