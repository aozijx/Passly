package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.core.crypto.KeyDerivation
import com.aozijx.passly.feature.backup.contract.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.archive.format.encrypted.BackupZipCodec
import com.aozijx.passly.feature.backup.internal.archive.format.encrypted.EncryptedBackupExporter
import com.aozijx.passly.feature.backup.internal.archive.format.encrypted.EncryptedBackupImporter
import com.aozijx.passly.feature.backup.internal.archive.io.LimitedInputStream
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldValue
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceRecord
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    @Test
    fun zipRoundTrip_preservesDocumentAndResources() {
        val document = """{"entries":[],"resources":[]}""".toByteArray()
        val resources =
            mapOf(
                "resources/icon_1" to byteArrayOf(1, 2, 3),
                "resources/attachment_2" to byteArrayOf(4, 5, 6, 7),
            )
        val encoded = ByteArrayOutputStream()
        BackupZipCodec.buildZip(document, resources, encoded)

        val decoded = BackupZipCodec.readZip(ByteArrayInputStream(encoded.toByteArray()))

        assertArrayEquals(document, decoded.documentJson)
        resources.forEach { (name, content) -> assertArrayEquals(content, decoded.resources[name]) }
    }

    @Test
    fun zipRead_emptyArchiveAndUnsafeResourceName_throw() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupZipCodec.readZip(ByteArrayInputStream(byteArrayOf()))
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackupZipCodec.buildZip(
                "{}".toByteArray(),
                mapOf("../outside" to byteArrayOf(1)),
                ByteArrayOutputStream(),
            )
        }
    }

    @Test
    fun limitedInputStream_acceptsExactLimit_andRejectsOneExtraByte() {
        val exact = LimitedInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 3)
        assertArrayEquals(byteArrayOf(1, 2, 3), exact.readBytes())

        val oversized = LimitedInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)), 3)
        assertThrows(IllegalStateException::class.java) { oversized.readBytes() }
    }

    @Test
    fun encryptedArchiveRoundTrip_keepsDocumentAndResourcesInsideChunkedContainer() {
        val title = "private-bank-login"
        val resource = "private-attachment-content".toByteArray()
        val resourceId = "attachment_encrypted"
        val bundle =
            BackupBundle(
                document =
                    BackupDocument(
                        format = BackupDocument.FORMAT,
                        version = BackupDocument.CURRENT_VERSION,
                        exportedAt = 1,
                        entries =
                            listOf(
                                BackupEntryRecord(
                                    id = "encrypted-entry",
                                    type = "login",
                                    revision = 1,
                                    createdAt = 1,
                                    updatedAt = 1,
                                    fields =
                                        listOf(
                                            BackupFieldRecord("title", BackupFieldValue.text(title)),
                                            BackupFieldRecord("password", BackupFieldValue.text("private-password")),
                                        ),
                                    attachmentIds = listOf(resourceId),
                                ),
                            ),
                        resources =
                            listOf(
                                BackupResourceRecord(
                                    id = resourceId,
                                    entryId = "encrypted-entry",
                                    kind = BackupResourceKind.ATTACHMENT,
                                    size = resource.size.toLong(),
                                    sha256 = BackupBundleValidator.sha256Hex(resource),
                                ),
                            ),
                    ),
                resourceData = mapOf(resourceId to resource),
            )
        val password = "archive-password".toCharArray()
        val encoded = ByteArrayOutputStream()

        EncryptedBackupExporter().export(bundle, password, encoded, ::testKey)

        assertFalse(encoded.toByteArray().containsSequence(title.toByteArray()))
        assertFalse(encoded.toByteArray().containsSequence(resource))
        val restored =
            EncryptedBackupImporter().import(
                ByteArrayInputStream(encoded.toByteArray()),
                password,
                BackupImportStrategy.STRICT,
                ::testKey,
            )
        assertArrayEquals(resource, restored.resourceData[resourceId])
        assertTrue(restored.document.entries.single().fields.any { it.value == BackupFieldValue.text(title) })
    }

    @Test
    fun encryptedImporter_mapsMalformedContainerToBackupFailure() {
        assertThrows(BackupFailed::class.java) {
            EncryptedBackupImporter().import(ByteArrayInputStream(byteArrayOf(1, 2, 3)), "password".toCharArray())
        }
    }
}

private fun testKey(
    password: CharArray,
    salt: ByteArray,
    parameters: KeyDerivation.Argon2idParameters,
): ByteArray {
    val material = password.concatToString().toByteArray() + salt + byteArrayOf(parameters.version.toByte())
    return try {
        MessageDigest.getInstance("SHA-256").digest(material)
    } finally {
        material.fill(0)
    }
}

private fun ByteArray.containsSequence(sequence: ByteArray): Boolean =
    indexOfSubsequence(sequence) >= 0

private fun ByteArray.indexOfSubsequence(sequence: ByteArray): Int {
    if (sequence.isEmpty()) return 0
    for (start in 0..size - sequence.size) {
        if (sequence.indices.all { this[start + it] == sequence[it] }) return start
    }
    return -1
}
