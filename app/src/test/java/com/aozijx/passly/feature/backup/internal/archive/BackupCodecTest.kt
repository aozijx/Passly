package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.core.crypto.KeyDerivation
import com.aozijx.passly.feature.backup.internal.archive.format.BackupFormatRegistry
import com.aozijx.passly.feature.backup.internal.archive.format.bitwarden.BitwardenJsonImportAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.encrypted.BackupArchiveCodec
import com.aozijx.passly.feature.backup.internal.archive.format.encrypted.EncryptedBackupContainerCodec
import com.aozijx.passly.feature.backup.internal.archive.format.encrypted.EncryptedBackupImporter
import com.aozijx.passly.feature.backup.internal.archive.format.json.JsonBackupExporter
import com.aozijx.passly.feature.backup.internal.archive.format.json.JsonBackupImporter
import com.aozijx.passly.feature.backup.internal.archive.format.json.PasslyJsonFormatAdapter
import com.aozijx.passly.feature.backup.internal.archive.io.LimitedInputStream
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldValue
import com.aozijx.passly.feature.backup.internal.archive.model.BackupLinkRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceRecord
import com.aozijx.passly.feature.backup.internal.model.BackupFormats
import com.aozijx.passly.feature.backup.internal.model.BackupImportStrategy
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.security.MessageDigest

class BackupCodecTest {

    @Test
    fun zipRoundTrip_preservesDocumentAndResources() {
        val documentJson = """{"entries":[],"resources":[]}""".toByteArray()
        val resources = mapOf(
            "resources/icon_1" to byteArrayOf(0x01, 0x02, 0x03),
            "resources/attachment_2" to byteArrayOf(0x10, 0x20, 0x30, 0x40)
        )

        val content = BackupArchiveCodec.readZip(
            BackupArchiveCodec.buildZip(documentJson, resources)
        )

        assertArrayEquals(documentJson, content.documentJson)
        assertEquals(resources.keys, content.resources.keys)
        resources.forEach { (key, value) ->
            assertArrayEquals(value, content.resources[key])
        }
    }

    @Test
    fun zipRead_emptyOrUnknownEntry_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupArchiveCodec.readZip(byteArrayOf())
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackupArchiveCodec.buildZip(
                "{}".toByteArray(),
                mapOf("../outside" to byteArrayOf(1))
            )
        }
    }

    @Test
    fun limitedInputStream_acceptsExactLimit_andRejectsOneExtraByte() {
        val exact = LimitedInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 3)
        assertArrayEquals(byteArrayOf(1, 2, 3), exact.readBytes())

        val oversized = LimitedInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)), 3)
        assertThrows(IllegalStateException::class.java) {
            oversized.readBytes()
        }
    }

    @Test
    fun containerRoundTrip_isSelfDescribing() {
        val password = "correct horse battery staple".toCharArray()
        val plaintext = """{"data":"test"}""".toByteArray()
        val parameters = KeyDerivation.Argon2idParameters(
            iterations = 5,
            memoryKiB = 32_768,
            parallelism = 2
        )

        val encrypted = EncryptedBackupContainerCodec.encrypt(
            plaintext,
            password,
            parameters,
            ::stubDeriveKey
        )
        val header = EncryptedBackupContainerCodec.inspectHeader(encrypted)
        val decrypted = EncryptedBackupContainerCodec.decrypt(
            encrypted,
            password,
            ::stubDeriveKey
        )

        assertEquals(EncryptedBackupContainerCodec.FORMAT_VERSION, header.formatVersion)
        assertEquals(EncryptedBackupContainerCodec.KDF_ARGON2ID, header.kdfId)
        assertEquals(EncryptedBackupContainerCodec.CIPHER_AES_256_GCM, header.cipherId)
        assertEquals(parameters, header.kdfParameters)
        assertEquals(128, header.tagLengthBits)
        assertEquals(12, header.nonce.size)
        assertEquals(plaintext.size + 16, header.ciphertextLength)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun containerV1_usesFreshSaltAndNonce_andRejectsExperimentalMagic() {
        val password = "unique-nonce-password".toCharArray()
        val first = EncryptedBackupContainerCodec.encrypt(
            "same-content".toByteArray(),
            password,
            deriveKey = ::stubDeriveKey
        )
        val second = EncryptedBackupContainerCodec.encrypt(
            "same-content".toByteArray(),
            password,
            deriveKey = ::stubDeriveKey
        )
        val firstHeader = EncryptedBackupContainerCodec.inspectHeader(first)
        val secondHeader = EncryptedBackupContainerCodec.inspectHeader(second)

        assertEquals(1, firstHeader.formatVersion)
        assertFalse(firstHeader.salt.contentEquals(secondHeader.salt))
        assertFalse(firstHeader.nonce.contentEquals(secondHeader.nonce))
        assertFalse(first.contentEquals(second))

        val experimental = first.copyOf()
        "PASSLYBK".toByteArray().copyInto(experimental)
        assertFalse(EncryptedBackupContainerCodec.hasMagic(experimental))
        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackupContainerCodec.decrypt(
                experimental,
                password,
                ::stubDeriveKey
            )
        }
    }

    @Test
    fun containerDecrypt_wrongPasswordOrCiphertextTamper_throws() {
        val encrypted = EncryptedBackupContainerCodec.encrypt(
            "secret".toByteArray(),
            "correct".toCharArray(),
            deriveKey = ::stubDeriveKey
        )
        assertThrows(Exception::class.java) {
            EncryptedBackupContainerCodec.decrypt(
                encrypted,
                "wrong".toCharArray(),
                ::stubDeriveKey
            )
        }

        val tampered = encrypted.copyOf()
        tampered[tampered.lastIndex] = (tampered.last().toInt() xor 1).toByte()
        assertThrows(Exception::class.java) {
            EncryptedBackupContainerCodec.decrypt(
                tampered,
                "correct".toCharArray(),
                ::stubDeriveKey
            )
        }
    }

    @Test
    fun containerDecrypt_authenticatedHeaderTamper_throws() {
        val password = "password".toCharArray()
        val encrypted = EncryptedBackupContainerCodec.encrypt(
            "authenticated".toByteArray(),
            password,
            deriveKey = ::stubDeriveKeyIgnoringParameters
        )
        val tampered = encrypted.copyOf()
        // iterations 位于 magic/version/headerLength/kdfId/cipherId/argonVersion 之后。
        val iterationsOffset = 8 + 4 + 4 + 4 + 4 + 4
        ByteBuffer.wrap(tampered).putInt(iterationsOffset, 4)

        assertThrows(Exception::class.java) {
            EncryptedBackupContainerCodec.decrypt(
                tampered,
                password,
                ::stubDeriveKeyIgnoringParameters
            )
        }
    }

    @Test
    fun containerDecrypt_lengthMismatchOrTrailingData_throwsBeforeKdf() {
        val encrypted = EncryptedBackupContainerCodec.encrypt(
            "data".toByteArray(),
            "password".toCharArray(),
            deriveKey = ::stubDeriveKey
        )
        val invalidLength = encrypted.copyOf()
        val ciphertextLengthOffset = 8 + 4 + 4 + 10 * 4
        ByteBuffer.wrap(invalidLength).putInt(
            ciphertextLengthOffset,
            ByteBuffer.wrap(invalidLength).getInt(ciphertextLengthOffset) + 1
        )
        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackupContainerCodec.decrypt(
                invalidLength,
                "password".toCharArray(),
                ::stubDeriveKey
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackupContainerCodec.decrypt(
                encrypted + byteArrayOf(0),
                "password".toCharArray(),
                ::stubDeriveKey
            )
        }
    }

    @Test
    fun containerRejectsDangerousKdfParameters_beforeDerivation() {
        val encrypted = EncryptedBackupContainerCodec.encrypt(
            "data".toByteArray(),
            "password".toCharArray(),
            deriveKey = ::stubDeriveKey
        )
        val dangerous = encrypted.copyOf()
        val memoryKiBOffset = 8 + 4 + 4 + 4 + 4 + 4 + 4
        ByteBuffer.wrap(dangerous).putInt(memoryKiBOffset, 512 * 1024)
        var derivationCalled = false

        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackupContainerCodec.decrypt(
                dangerous,
                "password".toCharArray()
            ) { _, _, _ ->
                derivationCalled = true
                ByteArray(32)
            }
        }
        assertFalse(derivationCalled)
    }

    @Test
    fun containerClearsDerivedKey_whenDeriverReturnsInvalidLength() {
        val invalidKey = ByteArray(31) { 0x5a.toByte() }

        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackupContainerCodec.encrypt(
                "data".toByteArray(),
                "password".toCharArray()
            ) { _, _, _ -> invalidKey }
        }
        assertTrue(invalidKey.all { it == 0.toByte() })
    }

    @Test
    fun encryptedArchiveRoundTrip_encryptsDocumentAndResourcePayloads() {
        val sensitiveTitle = "private-bank-login"
        val resourceContent = "private-attachment-content".toByteArray()
        val resourceId = "attachment_encrypted"
        val document = BackupDocument(
            format = BackupDocument.FORMAT,
            version = BackupDocument.CURRENT_VERSION,
            exportedAt = 1,
            entries = listOf(
                BackupEntryRecord(
                    id = "encrypted-entry",
                    type = "login",
                    revision = 1,
                    createdAt = 1,
                    updatedAt = 1,
                    fields = listOf(
                        BackupFieldRecord("title", BackupFieldValue.text(sensitiveTitle)),
                    ),
                    attachmentIds = listOf(resourceId)
                )
            ),
            resources = listOf(
                BackupResourceRecord(
                    id = resourceId,
                    entryId = "encrypted-entry",
                    kind = BackupResourceKind.ATTACHMENT,
                    size = resourceContent.size.toLong(),
                    sha256 = BackupBundleValidator.sha256Hex(resourceContent)
                )
            )
        )
        val documentJson = BackupJson.encodeToString(document).toByteArray()
        val zip = BackupArchiveCodec.buildZip(
            documentJson,
            mapOf("${BackupArchiveCodec.RESOURCE_ENTRY_PREFIX}$resourceId" to resourceContent)
        )
        val password = "archive-password".toCharArray()
        val encrypted = EncryptedBackupContainerCodec.encrypt(
            zip,
            password,
            deriveKey = ::stubDeriveKey
        )

        assertFalse(encrypted.containsSequence(sensitiveTitle.toByteArray()))
        assertFalse(encrypted.containsSequence(resourceContent))

        val decryptedZip = EncryptedBackupContainerCodec.decrypt(
            encrypted,
            password,
            ::stubDeriveKey
        )
        val restored = BackupArchiveCodec.readZip(decryptedZip)
        assertArrayEquals(documentJson, restored.documentJson)
        assertArrayEquals(
            resourceContent,
            restored.resources["${BackupArchiveCodec.RESOURCE_ENTRY_PREFIX}$resourceId"]
        )
    }

    @Test
    fun encryptedImporter_mapsMalformedContainerToBackupFailure() {
        assertThrows(BackupFailed::class.java) {
            EncryptedBackupImporter().import(
                byteArrayOf(1, 2, 3),
                "password".toCharArray()
            )
        }
    }

    @Test
    fun bitwardenAdapter_isAutoDetected_andMapsLoginTotpAndFolder() {
        val payload = """
            {
              "encrypted": false,
              "folders": [{"id":"folder-1","name":"Finance"}],
              "items": [{
                "id":"52A4DFB0-F19E-4C9D-82A1-BBEE95BBEF81",
                "folderId":"folder-1",
                "type":1,
                "name":"Bank",
                "favorite":true,
                "notes":"recovery note",
                "fields":[{"name":"PIN","value":"1234","type":1}],
                "login":{
                  "uris":[{"uri":"https://bank.example"}],
                  "username":"alice",
                  "password":"secret",
                  "totp":"otpauth://totp/Bank:alice?secret=JBSWY3DPEHPK3PXP&issuer=Bank&algorithm=SHA1&digits=6&period=30"
                },
                "creationDate":"2025-01-01T00:00:00Z",
                "revisionDate":"2025-01-02T00:00:00Z"
              }]
            }
        """.trimIndent().toByteArray()
        val adapter = BitwardenJsonImportAdapter()
        val registry = BackupFormatRegistry(
            exporters = emptySet(),
            importers = setOf(adapter)
        )

        val detected = registry.importer(requestedFormat = null, payload = payload)
        val bundle = detected.decode(payload, password = null)
        val account = bundle.document.entries.single { it.type == "account" }
        val login = bundle.document.entries.single { it.type == "login" }
        val otp = bundle.document.entries.single { it.type == "otp" }

        assertEquals(BackupFormats.BITWARDEN_JSON, detected.formatId)
        assertEquals("Bank", account.field("title").text)
        assertTrue(bundle.document.links.any {
            it.sourceEntryId == login.id &&
                it.targetEntryId == account.id &&
                it.relationType == "member_of_account"
        })
        assertTrue(bundle.document.links.any {
            it.sourceEntryId == otp.id &&
                it.targetEntryId == login.id &&
                it.relationType == "otp_for"
        })
        assertEquals("alice", login.field("username").text)
        assertEquals(listOf("Finance"), login.field("tags").texts)
        assertEquals("secret", login.field("password").text)
        assertEquals("1234", login.field("custom_fields").customFields?.single()?.value)
        assertEquals("JBSWY3DPEHPK3PXP", otp.field("otp_secret").text)
        assertEquals("Bank", otp.field("otp_issuer").text)
    }

    @Test
    fun registry_prefersPasslySignature_overIncidentalItemsText() {
        val passlyAdapter = PasslyJsonFormatAdapter(
            JsonBackupExporter(),
            JsonBackupImporter()
        )
        val bitwardenAdapter = BitwardenJsonImportAdapter()
        val registry = BackupFormatRegistry(
            exporters = emptySet(),
            importers = setOf(passlyAdapter, bitwardenAdapter)
        )
        val payload = JsonBackupExporter().export(
            BackupBundle(
                BackupDocument(
                    format = BackupDocument.FORMAT,
                    version = BackupDocument.CURRENT_VERSION,
                    exportedAt = 1,
                    entries = listOf(
                        BackupEntryRecord(
                            id = "probe-entry",
                            type = "note",
                            revision = 1,
                            createdAt = 1,
                            updatedAt = 1,
                            fields = listOf(
                                BackupFieldRecord("title", BackupFieldValue.text("Probe")),
                                BackupFieldRecord("notes", BackupFieldValue.text("\"items\"")),
                            ),
                        )
                    )
                )
            )
        ).toByteArray()

        assertEquals(
            BackupFormats.PASSLY_JSON,
            registry.importer(requestedFormat = null, payload = payload).formatId
        )
    }

    @Test
    fun bitwardenAdapter_rejectsEncryptedAndUnsupportedItems() {
        val adapter = BitwardenJsonImportAdapter()
        assertThrows(BackupFailed::class.java) {
            adapter.decode(
                """{"encrypted":true,"items":[]}""".toByteArray(),
                password = null
            )
        }
        assertThrows(BackupFailed::class.java) {
            adapter.decode(
                """{"encrypted":false,"items":[{"type":5,"name":"SSH","sshKey":{}}]}"""
                    .toByteArray(),
                password = null
            )
        }
    }

    @Test
    fun backupJson_omitsNullsAndDefaults_butKeepsFormatIdentity() {
        val json = BackupJson.encodeToString(
            BackupDocument(
                format = BackupDocument.FORMAT,
                version = BackupDocument.CURRENT_VERSION,
                exportedAt = 0L,
                entries = emptyList()
            )
        )

        assertFalse(json.contains("null"))
        assertTrue(json.contains("\"format\""))
        assertTrue(json.contains("\"version\""))
        assertTrue(json.contains("\"entries\""))
        assertFalse(json.contains("\"resources\""))
    }

    @Test
    fun fieldArchiveV1_wireFieldNamesAreStableAndIndependentFromDatabasePayloads() {
        val document = BackupDocument(
            format = BackupDocument.FORMAT,
            version = BackupDocument.CURRENT_VERSION,
            exportedAt = 1,
            entries = listOf(
                BackupEntryRecord(
                    id = "wire-entry",
                    type = "login",
                    revision = 1,
                    createdAt = 1,
                    updatedAt = 2,
                    fields = listOf(
                        BackupFieldRecord("title", BackupFieldValue.text("Title")),
                        BackupFieldRecord("username", BackupFieldValue.text("alice")),
                        BackupFieldRecord("primary_url", BackupFieldValue.text("https://example.com")),
                        BackupFieldRecord("password", BackupFieldValue.text("secret")),
                    ),
                )
            )
        )

        val root = BackupJson.parseToJsonElement(
            BackupJson.encodeToString(document)
        ).jsonObject
        val entry = root.getValue("entries").jsonArray.single().jsonObject
        assertEquals(
            setOf(
                "id",
                "type",
                "revision",
                "createdAt",
                "updatedAt",
                "fields",
            ),
            entry.keys
        )
        assertEquals(4, entry.getValue("fields").jsonArray.size)
        assertFalse(BackupJson.encodeToString(document).contains("schemaVersion"))
        assertFalse(BackupJson.encodeToString(document).contains("summary"))
        assertFalse(BackupJson.encodeToString(document).contains("\"secret\":"))
    }

    @Test
    fun documentValidator_rejectsUnknownVersion() {
        val bundle = BackupBundle(
            BackupDocument(
                format = BackupDocument.FORMAT,
                version = BackupDocument.CURRENT_VERSION + 1,
                exportedAt = 1,
                entries = emptyList()
            )
        )

        assertThrows(IllegalArgumentException::class.java) {
            BackupBundleValidator.validate(bundle, requireResourceData = false)
        }
    }

    @Test
    fun documentValidator_acceptsAccountHierarchy_andRejectsMixedPayload() {
        val account = BackupEntryRecord(
            id = "account-1",
            type = "account",
            revision = 1,
            createdAt = 1,
            updatedAt = 1,
            fields = listOf(BackupFieldRecord("title", BackupFieldValue.text("Example"))),
        )
        val login = BackupEntryRecord(
            id = "login-1",
            type = "login",
            revision = 1,
            createdAt = 1,
            updatedAt = 1,
            fields = listOf(
                BackupFieldRecord("title", BackupFieldValue.text("Example login")),
                BackupFieldRecord("username", BackupFieldValue.text("alice")),
                BackupFieldRecord("password", BackupFieldValue.text("secret")),
            ),
        )
        val valid = BackupBundle(
            BackupDocument(
                format = BackupDocument.FORMAT,
                version = BackupDocument.CURRENT_VERSION,
                exportedAt = 1,
                entries = listOf(login, account),
                links = listOf(
                    BackupLinkRecord(
                        id = "login-account-link",
                        sourceEntryId = login.id,
                        targetEntryId = account.id,
                        relationType = "member_of_account",
                        createdAt = 1,
                        updatedAt = 1
                    )
                )
            )
        )

        BackupBundleValidator.validate(valid, requireResourceData = false)
        val json = JsonBackupExporter().export(valid)
        val restored = JsonBackupImporter().import(json.toByteArray())
        assertEquals(valid.document.links, restored.document.links)

        val mixed = login.copy(fields = login.fields + BackupFieldRecord(
            "otp_secret",
            BackupFieldValue.text("JBSWY3DPEHPK3PXP"),
        ))
        assertThrows(IllegalArgumentException::class.java) {
            BackupBundleValidator.validate(
                valid.copy(document = valid.document.copy(entries = listOf(account, mixed))),
                requireResourceData = false
            )
        }

        val reversedMembership = valid.document.links.single().copy(
            sourceEntryId = account.id,
            targetEntryId = login.id,
        )
        assertThrows(IllegalArgumentException::class.java) {
            BackupBundleValidator.validate(
                valid.copy(document = valid.document.copy(links = listOf(reversedMembership))),
                requireResourceData = false,
            )
        }

        val legacyVaultFormat = json.replace(BackupDocument.FORMAT, "passly-vault")
        assertThrows(Exception::class.java) {
            JsonBackupImporter().import(legacyVaultFormat.toByteArray())
        }
    }

    @Test
    fun jsonRoundTrip_preservesResourceBytesAndIntegrity() {
        val content = byteArrayOf(1, 2, 3, 4, 5)
        val resource = BackupResourceRecord(
            id = "attachment_1",
            entryId = "json-test-id",
            kind = BackupResourceKind.ATTACHMENT,
            fileName = "note.bin",
            size = content.size.toLong(),
            sha256 = BackupBundleValidator.sha256Hex(content)
        )
        val entry = BackupEntryRecord(
            id = "json-test-id",
            type = "login",
            revision = 2,
            createdAt = 1000L,
            updatedAt = 2000L,
            fields = listOf(
                BackupFieldRecord("title", BackupFieldValue.text("JSON Test")),
                BackupFieldRecord("password", BackupFieldValue.text("secret")),
            ),
            attachmentIds = listOf(resource.id)
        )
        val bundle = BackupBundle(
            document = BackupDocument(
                format = BackupDocument.FORMAT,
                version = BackupDocument.CURRENT_VERSION,
                exportedAt = 3000L,
                entries = listOf(entry),
                resources = listOf(resource)
            ),
            resourceData = mapOf(resource.id to content)
        )

        val json = JsonBackupExporter().export(bundle)
        val restored = JsonBackupImporter().import(json.toByteArray())

        assertEquals(bundle.document, restored.document)
        assertArrayEquals(content, restored.resourceData[resource.id])
        assertFalse(json.contains("null"))
    }

    @Test
    fun jsonImport_tamperedResource_throws() {
        val validContent = byteArrayOf(1, 2, 3)
        val resource = BackupResourceRecord(
            id = "icon_entry-1",
            entryId = "entry-1",
            kind = BackupResourceKind.ICON,
            size = validContent.size.toLong(),
            sha256 = BackupBundleValidator.sha256Hex(validContent)
        )
        val bundle = BackupBundle(
            document = BackupDocument(
                format = BackupDocument.FORMAT,
                version = BackupDocument.CURRENT_VERSION,
                exportedAt = 1,
                entries = listOf(
                    BackupEntryRecord(
                        id = "entry-1",
                        type = "login",
                        revision = 1,
                        createdAt = 1,
                        updatedAt = 1,
                        fields = listOf(
                            BackupFieldRecord("title", BackupFieldValue.text("Entry")),
                            BackupFieldRecord("password", BackupFieldValue.text("secret")),
                        ),
                    )
                ),
                resources = listOf(resource)
            ),
            resourceData = mapOf(resource.id to validContent)
        )
        val json = JsonBackupExporter().export(bundle)
            .replace("AQID", "AQIE")

        assertThrows(IllegalArgumentException::class.java) {
            JsonBackupImporter().import(json.toByteArray())
        }
        assertThrows(IllegalArgumentException::class.java) {
            JsonBackupImporter().import(
                json.toByteArray(),
                BackupImportStrategy.COMPATIBLE,
            )
        }
    }

    private fun BackupEntryRecord.field(key: String): BackupFieldValue =
        fields.single { it.key == key }.value

    private fun stubDeriveKey(
        password: CharArray,
        salt: ByteArray,
        parameters: KeyDerivation.Argon2idParameters
    ): ByteArray {
        val hash = MessageDigest.getInstance("SHA-256")
        hash.update(String(password).toByteArray())
        hash.update(salt)
        hash.update(ByteBuffer.allocate(16).apply {
            putInt(parameters.iterations)
            putInt(parameters.memoryKiB)
            putInt(parameters.parallelism)
            putInt(parameters.keyLengthBits)
        }.array())
        return hash.digest()
    }

    private fun stubDeriveKeyIgnoringParameters(
        password: CharArray,
        salt: ByteArray,
        @Suppress("UNUSED_PARAMETER") parameters: KeyDerivation.Argon2idParameters
    ): ByteArray {
        val hash = MessageDigest.getInstance("SHA-256")
        hash.update(String(password).toByteArray())
        hash.update(salt)
        return hash.digest()
    }

    private fun ByteArray.containsSequence(sequence: ByteArray): Boolean {
        if (sequence.isEmpty()) return true
        return indices.any { start ->
            start + sequence.size <= size &&
                    sequence.indices.all { offset -> this[start + offset] == sequence[offset] }
        }
    }
}
