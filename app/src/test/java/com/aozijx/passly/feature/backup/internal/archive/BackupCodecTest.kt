package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.core.crypto.KeyDerivation
import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.feature.backup.contract.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.archive.format.encrypted.BackupZipCodec
import com.aozijx.passly.feature.backup.internal.archive.format.encrypted.ChunkedBackupOutputStream
import com.aozijx.passly.feature.backup.internal.archive.format.encrypted.EncryptedBackupExporter
import com.aozijx.passly.feature.backup.internal.archive.format.encrypted.EncryptedBackupImporter
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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    @Test
    fun encryptedImportReportsMissingZipDocument() {
        val zip = ByteArrayOutputStream()
        ZipOutputStream(zip).use { }
        val encoded = ByteArrayOutputStream()
        val password = "password".toCharArray()
        try {
            ChunkedBackupOutputStream(encoded, password, KeyDerivation.BASELINE_ARGON2ID_PARAMETERS, ::testKey).use { it.write(zip.toByteArray()) }
            val failure = assertThrows(BackupFailed::class.java) {
                EncryptedBackupImporter().import(ByteArrayInputStream(encoded.toByteArray()), password, BackupImportStrategy.STRICT, ::testKey)
            }
            org.junit.Assert.assertEquals(Reasons.BackupZipDocumentMissing.value, failure.reasonCode)
        } finally {
            password.fill('\u0000')
        }
    }

    @Test
    fun encryptedImportWrongPasswordReportsAuthenticationFailure() {
        val bundle = BackupBundle(
            BackupDocument(format = BackupDocument.FORMAT, version = BackupDocument.CURRENT_VERSION, exportedAt = 1, entries = emptyList()),
        )
        val encoded = ByteArrayOutputStream()
        val password = "correct".toCharArray()
        val wrongPassword = "incorrect".toCharArray()
        try {
            EncryptedBackupExporter().export(bundle, password, encoded, ::testKey)
            val failure = assertThrows(BackupFailed::class.java) {
                EncryptedBackupImporter().import(ByteArrayInputStream(encoded.toByteArray()), wrongPassword, BackupImportStrategy.STRICT, ::testKey)
            }
            org.junit.Assert.assertEquals(Reasons.BackupAuthenticationFailed.value, failure.reasonCode)
        } finally {
            password.fill('\u0000')
            wrongPassword.fill('\u0000')
        }
    }

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
        val missing = assertThrows(BackupIntegrityException::class.java) {
            BackupZipCodec.readZip(ByteArrayInputStream(byteArrayOf()))
        }
        org.junit.Assert.assertEquals(Reasons.BackupZipDocumentMissing, missing.reasonCode)
        val invalid = assertThrows(BackupIntegrityException::class.java) {
            BackupZipCodec.buildZip(
                "{}".toByteArray(),
                mapOf("../outside" to byteArrayOf(1)),
                ByteArrayOutputStream(),
            )
        }
        org.junit.Assert.assertEquals(Reasons.BackupZipInvalidEntry, invalid.reasonCode)
    }

    @Test
    fun zipRead_duplicateResourceName_rejectsArchive() {
        val encoded = ByteArrayOutputStream()
        ZipOutputStream(encoded).use { zip ->
            listOf("document.json", "resources/duplicate1", "resources/duplicate2").forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(byteArrayOf(1))
                zip.closeEntry()
            }
        }
        val archive = encoded.toByteArray()
        val secondName = "resources/duplicate2".toByteArray()
        val localNameOffset = archive.indexOfSubsequence(secondName)
        assertTrue(localNameOffset >= 0)
        archive[localNameOffset + secondName.lastIndex] = '1'.code.toByte()

        val failure = assertThrows(BackupIntegrityException::class.java) {
            BackupZipCodec.readZip(ByteArrayInputStream(archive))
        }
        org.junit.Assert.assertEquals(Reasons.BackupZipDuplicateEntry, failure.reasonCode)
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
        assertTrue(
            restored.document.entries
                .single()
                .fields
                .any { it.value == BackupFieldValue.text(title) },
        )
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

private fun ByteArray.containsSequence(sequence: ByteArray): Boolean = indexOfSubsequence(sequence) >= 0

private fun ByteArray.indexOfSubsequence(sequence: ByteArray): Int {
    if (sequence.isEmpty()) return 0
    for (start in 0..size - sequence.size) {
        if (sequence.indices.all { this[start + it] == sequence[it] }) return start
    }
    return -1
}
