package com.aozijx.passly.feature.backup.internal.archive.format.json

import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.feature.backup.contract.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.BackupIntegrityException
import com.aozijx.passly.feature.backup.internal.archive.BackupJson
import com.aozijx.passly.feature.backup.internal.archive.format.bitwarden.BitwardenJsonImportAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.bitwarden.BitwardenJsonParser
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldValue
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceRecord
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonBackupStreamTest {
    @Test
    fun autoDispatchRejectsResourceDigestMismatchInBothStrategies() {
        val content = byteArrayOf(1, 2, 3)
        val document = BackupDocument(
            format = BackupDocument.FORMAT,
            version = BackupDocument.CURRENT_VERSION,
            exportedAt = 1,
            entries = listOf(BackupEntryRecord(id = "entry", type = "login", revision = 1, createdAt = 1, updatedAt = 1, fields = listOf(BackupFieldRecord("title", BackupFieldValue.text("title"))))),
            resources = listOf(BackupResourceRecord(id = "icon", entryId = "entry", kind = BackupResourceKind.ICON, size = 3, sha256 = BackupBundleValidator.sha256Hex(content))),
        )
        val payload = """{"document":${BackupJson.encodeToString(document)},"resourcesBase64":{"icon":"AwIB"}}"""

        BackupImportStrategy.entries.forEach { strategy ->
            val failure = org.junit.Assert.assertThrows(BackupIntegrityException::class.java) {
                dispatcher().decode(ByteArrayInputStream(payload.toByteArray()), null, strategy)
            }
            assertEquals(Reasons.Backup.Resource.BackupResourceDigestMismatch, failure.reasonCode)
        }
    }

    @Test
    fun exporterAndImporterRoundTripThroughStreams() {
        val bundle = emptyBundle()
        val encoded = ByteArrayOutputStream()

        JsonBackupExporter().export(bundle, encoded)
        val imported = JsonBackupImporter().import(ByteArrayInputStream(encoded.toByteArray()))

        assertEquals(bundle.document, imported.document)
        assertTrue(imported.resourceData.isEmpty())
    }

    @Test
    fun importerUsesRootFields_andIgnoresNestedMarkerDecoys() {
        val document = BackupJson.encodeToString(emptyBundle().document)
        val payload = """{"resourcesBase64":{},"metadata":{"items":[]},"document":$document}"""

        val imported = JsonBackupImporter().import(ByteArrayInputStream(payload.toByteArray()), BackupImportStrategy.STRICT)

        assertEquals(BackupDocument.FORMAT, imported.document.format)
        assertTrue(imported.document.entries.isEmpty())
    }

    @Test
    fun importerRejectsDuplicateResourceId() {
        val document = BackupJson.encodeToString(emptyBundle().document)
        val payload = """{"resourcesBase64":{"duplicate":"AQ==","duplicate":"Ag=="},"document":$document}"""

        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            JsonBackupImporter().import(ByteArrayInputStream(payload.toByteArray()), BackupImportStrategy.STRICT)
        }
    }

    @Test
    fun dispatcherSelectsPasslyFromRootDocument_afterPrecedingResources() {
        val document = BackupJson.encodeToString(emptyBundle().document)
        val payload = """{"resourcesBase64":{},"metadata":{"items":[]},"document":$document}"""

        val imported = dispatcher().decode(ByteArrayInputStream(payload.toByteArray()), null, BackupImportStrategy.STRICT)

        assertEquals(BackupDocument.FORMAT, imported.document.format)
    }

    @Test
    fun dispatcherSelectsBitwardenFromRootItems_whenFoldersComeFirst() {
        val payload =
            """{"folders":[{"id":"folder-1","name":"Personal"}],"encrypted":false,"items":[{"type":1,"name":"Email","folderId":"folder-1","login":{"password":"test-secret"}}]}"""

        val imported = dispatcher().decode(ByteArrayInputStream(payload.toByteArray()), null, BackupImportStrategy.STRICT)

        val entry = imported.document.entries.single()
        assertEquals("login", entry.type)
        assertEquals("Personal", entry.fields.single { it.key == "tags" }.value.texts?.single())
    }

    @Test
    fun dispatcherRejectsAmbiguousRootMarkers() {
        val document = BackupJson.encodeToString(emptyBundle().document)
        val payload = """{"items":[],"document":$document}"""

        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            dispatcher().decode(ByteArrayInputStream(payload.toByteArray()), null, BackupImportStrategy.STRICT)
        }
    }

    private fun emptyBundle() =
        BackupBundle(
            BackupDocument(
                format = BackupDocument.FORMAT,
                version = BackupDocument.CURRENT_VERSION,
                exportedAt = 1,
                entries = emptyList(),
            ),
        )

    private fun dispatcher() =
        JsonFormatDispatcher(
            JsonBackupImporter(),
            BitwardenJsonImportAdapter(BitwardenJsonParser()),
        )
}
