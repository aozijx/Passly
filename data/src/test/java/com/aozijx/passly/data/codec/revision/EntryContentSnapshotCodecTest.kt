package com.aozijx.passly.data.codec.revision

import com.aozijx.passly.data.codec.DatabaseRecordAad
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.core.crypto.FieldEncryptor
import com.aozijx.passly.core.crypto.AesGcmCryptoEngine
import com.aozijx.passly.security.dek.FieldKeyManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryContentSnapshotCodecTest {
    @Test
    fun `content snapshot retains low sensitivity structure but redacts field values`() = runBlocking {
        withCodec { codec, _ ->
            val summary = EntryProfile(title = "Example", username = "person@example.com")
            val secret = EntrySecret(
                credential = LoginCredential(password = "secret"),
                notes = "repeated ".repeat(2_000),
            )
            val link = EntryLink.create(
                id = EntryLinkId("link-1"),
                sourceEntryId = EntryId(ENTRY_ID),
                targetEntryId = EntryId("target-entry"),
                relationType = EntryRelationType.OTP_FOR,
                createdAt = 100L,
            )
            val encrypted = codec.encrypt(summary, secret, ENTRY_ID, listOf(link))
            val decoded = codec.decrypt(encrypted, ENTRY_ID)

            assertEquals(summary, decoded.summary)
            assertEquals("repeated ".repeat(2_000), decoded.secret.notes)
            assertNull(decoded.secret.login?.password)
            assertEquals(listOf(link), decoded.links)
        }
    }

    @Test
    fun `content snapshot writes only the current format identifier`() = runBlocking {
        withCodec { codec, encryptor ->
            val encrypted = codec.encrypt(
                summary = EntryProfile(title = "Example"),
                bundleSecret = EntrySecret(credential = LoginCredential(password = "secret")),
                entryId = ENTRY_ID,
                links = emptyList(),
            )

            val encoded = encryptor.decrypt(encrypted, DatabaseRecordAad.revision(ENTRY_ID))

            assertTrue(encoded.startsWith("content2:"))
        }
    }

    private suspend fun withCodec(
        block: suspend (EntryContentSnapshotCodec, FieldEncryptor) -> Unit,
    ) {
        val keyManager = FieldKeyManager().apply {
            deriveAndSet(ByteArray(32) { (it + 3).toByte() })
        }
        try {
            val encryptor = FieldEncryptor(keyManager, AesGcmCryptoEngine())
            block(EntryContentSnapshotCodec(encryptor), encryptor)
        } finally {
            keyManager.clear()
        }
    }

    private companion object {
        const val ENTRY_ID = "018f9dd6-66c5-7cc0-85b5-39a337956681"
    }
}
