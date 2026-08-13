package com.aozijx.passly.data.codec.revision

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
import org.junit.Test

class EntryContentSnapshotCodecTest {
    @Test
    fun `versioned compressed content snapshot round trips without attachment ownership`() = runBlocking {
        withCodec { codec ->
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
            assertEquals(secret, decoded.secret)
            assertEquals(listOf(link), decoded.links)
        }
    }

    private suspend fun withCodec(block: suspend (EntryContentSnapshotCodec) -> Unit) {
        val keyManager = FieldKeyManager().apply {
            deriveAndSet(ByteArray(32) { (it + 3).toByte() })
        }
        try {
            block(EntryContentSnapshotCodec(FieldEncryptor(keyManager, AesGcmCryptoEngine())))
        } finally {
            keyManager.clear()
        }
    }

    private companion object {
        const val ENTRY_ID = "018f9dd6-66c5-7cc0-85b5-39a337956681"
    }
}
