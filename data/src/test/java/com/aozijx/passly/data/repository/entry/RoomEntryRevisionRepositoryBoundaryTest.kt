package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.data.local.database.entity.EntryRevisionEntity
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveRevisionAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationPermitVerifier
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.OwnedChars
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomEntryRevisionRepositoryBoundaryTest {
    @Test
    fun `metadata mapping never needs encrypted snapshot content`() {
        val entity =
            EntryRevisionEntity(
                revisionId = "revision-7",
                entryId = "entry-1",
                version = 7,
                entryContentCipher = byteArrayOf(),
                sensitiveFieldCipherSet = byteArrayOf(),
                changeType = RevisionChange.VERSION_RESTORED.name,
                createdAt = 42L,
            )

        val metadata = entity.toRevisionMetadata()

        assertEquals(EntryRevisionId("revision-7"), metadata.id)
        assertEquals(EntryId("entry-1"), metadata.entryId)
        assertEquals(7, metadata.version.value)
        assertEquals(42L, metadata.createdAtMs)
        assertEquals(RevisionChange.VERSION_RESTORED, metadata.change)
    }

    @Test
    fun `reveal permit is consumed for the exact revision key set`() {
        val verifier = RecordingPermitVerifier()
        val consumer = RevisionPermitConsumer(verifier)
        val permit = TestPermit
        val keys = setOf(SensitiveFieldKey.PASSWORD, SensitiveFieldKey.OTP_SECRET)

        assertTrue(
            consumer.consumeReveal(
                permit = permit,
                entryId = EntryId("entry-1"),
                revisionId = EntryRevisionId("revision-7"),
                keys = keys,
            ),
        )
        assertEquals(
            AuthorizationScope.SensitiveRevision(
                entryId = EntryId("entry-1"),
                revisionId = "revision-7",
                fieldKeys = keys,
                action = SensitiveRevisionAccessAction.REVEAL,
            ),
            verifier.consumedScope,
        )
    }

    @Test
    fun `partially built sensitive results are wiped when a later field fails`() =
        runTest {
            val created = mutableListOf<OwnedChars>()
            val failure = IllegalStateException("second field decrypt failed")

            val actualFailure =
                runCatching {
                    buildSensitiveResults(
                        keys = listOf("first", "second"),
                        create = { key ->
                            if (key == "second") throw failure
                            OwnedChars.fromString("first-value").also(created::add)
                        },
                        assemble = { _, value -> value },
                    )
                }.exceptionOrNull()

            assertSame(failure, actualFailure)
            assertEquals(1, created.size)
            assertTrue(created.single().isWiped)
        }

    @Test
    fun `created sensitive values are wiped when result assembly fails`() =
        runTest {
            val created = mutableListOf<OwnedChars>()
            val failure = IllegalStateException("result assembly failed")

            val actualFailure =
                runCatching {
                    buildSensitiveResults(
                        keys = listOf("first", "second"),
                        create = { key -> OwnedChars.fromString(key).also(created::add) },
                        assemble = { key, value ->
                            if (key == "second") throw failure
                            value
                        },
                    )
                }.exceptionOrNull()

            assertSame(failure, actualFailure)
            assertEquals(2, created.size)
            assertTrue(created.all(OwnedChars::isWiped))
        }

    private object TestPermit : AuthorizationPermit

    private class RecordingPermitVerifier : AuthorizationPermitVerifier {
        var consumedScope: AuthorizationScope? = null

        override fun consume(
            permit: AuthorizationPermit,
            expectedScope: AuthorizationScope,
        ): Boolean {
            consumedScope = expectedScope
            return permit === TestPermit
        }
    }
}
