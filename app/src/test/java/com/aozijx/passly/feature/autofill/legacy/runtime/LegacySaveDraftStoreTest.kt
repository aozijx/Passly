package com.aozijx.passly.feature.autofill.legacy.runtime

import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LegacySaveDraftStoreTest {
    @Test
    fun consumeIsAtomicAndSingleUse() = runBlocking {
        val store = store()
        val token = store.stage(draft("secret"), scope())

        val consumed = listOf(
            async { store.consume(token) },
            async { store.consume(token) },
        ).awaitAll()

        assertEquals(1, consumed.count { it != null })
        assertEquals("secret", consumed.single { it != null }?.draft?.password)
    }

    @Test
    fun entryExpiresAtExactlySixtySeconds() = runBlocking {
        var now = 1_000L
        val store = store(nowMillis = { now })
        val token = store.stage(draft(), scope())

        now += LegacySaveDraftStore.TTL_MILLIS

        assertNull(store.consume(token))
    }

    @Test
    fun removeDiscardsEntry() = runBlocking {
        val store = store()
        val token = store.stage(draft(), scope())

        store.remove(token)

        assertNull(store.consume(token))
    }

    @Test
    fun stagingPrunesExpiredEntriesBeforeCapacityEviction() = runBlocking {
        var now = 0L
        var next = 0
        val store = store(nowMillis = { now }, tokenFactory = { LegacySaveToken("t-${++next}") })
        val expired = store.stage(draft("expired"), scope())
        now = LegacySaveDraftStore.TTL_MILLIS

        val live = (1..LegacySaveDraftStore.MAX_ENTRIES).map {
            store.stage(draft("live-$it"), scope())
        }

        assertNull(store.consume(expired))
        live.forEach { assertNotNull(store.consume(it)) }
    }

    @Test
    fun ninthLiveEntryEvictsOldest() = runBlocking {
        var next = 0
        val store = store(tokenFactory = { LegacySaveToken("t-${++next}") })

        val tokens = (1..9).map { store.stage(draft("secret-$it"), scope()) }

        assertNull(store.consume(tokens.first()))
        tokens.drop(1).forEach { assertNotNull(store.consume(it)) }
    }

    @Test
    fun tokenStringNeverContainsDraftFields() = runBlocking {
        val store = store(tokenFactory = { LegacySaveToken("opaque-token") })
        val token = store.stage(draft("highly-secret"), scope("com.secret.app"))

        assertFalse(token.toString().contains("highly-secret"))
        assertFalse(token.toString().contains("com.secret.app"))
    }

    private fun store(
        nowMillis: () -> Long = { 0L },
        tokenFactory: () -> LegacySaveToken = { LegacySaveToken("token") },
    ) = LegacySaveDraftStore(nowMillis, tokenFactory)

    private fun draft(password: String = "password") = AutofillSaveDraft(
        packageName = "com.example",
        webDomain = "example.com",
        pageTitle = null,
        username = "user",
        password = password,
    )

    private fun scope(packageName: String = "com.example") =
        AutofillGrantContext(packageName, "example.com")
}
