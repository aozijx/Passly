package com.aozijx.passly.data.codec.revision

import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.link.EntryLink
import com.aozijx.passly.domain.entry.model.link.EntryLinkId
import com.aozijx.passly.domain.entry.model.link.EntryRelationType
import com.aozijx.passly.domain.entry.model.secret.LoginSecret
import com.aozijx.passly.security.crypto.FieldEncryptor
import com.aozijx.passly.security.crypto.SessionKeyManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryRevisionCodecTest {

    @Test
    fun `versioned compressed snapshot round trips`() = runBlocking {
        withCodec { codec ->
            val summary = EntrySummary(title = "Example", username = "person@example.com")
            val secret = EntrySecret(
                login = LoginSecret(password = "secret"),
                notes = "repeated ".repeat(2_000),
            )

            val link = EntryLink.create(
                id = EntryLinkId("link-1"),
                sourceEntryId = EntryId(ENTRY_ID),
                targetEntryId = EntryId("target-entry"),
                relationType = EntryRelationType.OTP_FOR,
                createdAt = 100L,
            )
            val encrypted = codec.encrypt(
                summary = summary,
                secret = secret,
                entryId = ENTRY_ID,
                links = listOf(link),
                attachmentIds = listOf("attachment-2", "attachment-1"),
            )
            val decoded = codec.decrypt(encrypted, ENTRY_ID)

            assertEquals(summary, decoded.summary)
            assertEquals(secret, decoded.secret)
            assertEquals(listOf(link), decoded.links)
            assertEquals(listOf("attachment-1", "attachment-2"), decoded.attachmentIds)
        }
    }

    private suspend fun withCodec(
        block: suspend (EntryRevisionCodec) -> Unit,
    ) {
        val keyManager = SessionKeyManager().apply {
            deriveAndSet(ByteArray(32) { (it + 3).toByte() })
        }
        val encryptor = FieldEncryptor(keyManager)
        try {
            block(EntryRevisionCodec(encryptor))
        } finally {
            keyManager.clearSessionKey()
        }
    }

    private companion object {
        const val ENTRY_ID = "018f9dd6-66c5-7cc0-85b5-39a337956681"
    }
}
