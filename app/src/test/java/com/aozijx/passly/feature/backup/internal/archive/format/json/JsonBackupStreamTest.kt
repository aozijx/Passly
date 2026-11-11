package com.aozijx.passly.feature.backup.internal.archive.format.json

import com.aozijx.passly.feature.backup.contract.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.archive.BackupJson
import com.aozijx.passly.feature.backup.internal.archive.format.bitwarden.BitwardenJsonImportAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.bitwarden.BitwardenJsonParser
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonBackupStreamTest {
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
    fun dispatcherSelectsPasslyFromRootDocument_afterPrecedingResources() {
        val document = BackupJson.encodeToString(emptyBundle().document)
        val payload = """{"resourcesBase64":{},"metadata":{"items":[]},"document":$document}"""

        val imported = dispatcher().decode(ByteArrayInputStream(payload.toByteArray()), null, BackupImportStrategy.STRICT)

        assertEquals(BackupDocument.FORMAT, imported.document.format)
    }

    @Test
    fun dispatcherSelectsBitwardenFromRootItems_whenFoldersComeFirst() {
        val payload =
            """{"folders":[{"id":"folder-1","name":"Personal"}],"encrypted":false,"items":[{"type":1,"name":"Email","folderId":"folder-1"}]}"""

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
