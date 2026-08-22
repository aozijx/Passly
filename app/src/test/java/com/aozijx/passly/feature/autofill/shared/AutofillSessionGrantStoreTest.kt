package com.aozijx.passly.feature.autofill.shared

import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class AutofillSessionGrantStoreTest {

    private var now = 0L

    private fun store(ttl: Long = 30_000L) = AutofillSessionGrantStore(
        ttlMillis = ttl,
        elapsedRealtime = { now },
    )

    @Test
    fun `grant covers the same package and domain`() {
        val store = store()
        store.grant(AutofillGrantContext("com.example.app", "accounts.google.com"))

        assertTrue(
            store.isGranted(AutofillGrantContext("com.example.app", "accounts.google.com"))
        )
    }

    @Test
    fun `grant does not cover a different package`() {
        val store = store()
        store.grant(AutofillGrantContext("com.example.app", "accounts.google.com"))

        assertFalse(
            store.isGranted(AutofillGrantContext("com.example.other", "accounts.google.com"))
        )
    }

    @Test
    fun `grant does not cover a different domain`() {
        val store = store()
        store.grant(AutofillGrantContext("com.example.app", "accounts.google.com"))

        assertFalse(
            store.isGranted(AutofillGrantContext("com.example.app", "mail.google.com"))
        )
    }

    @Test
    fun `grant expires after ttl`() {
        val store = store(ttl = 30_000L)
        store.grant(AutofillGrantContext("com.example.app", null))
        now = 29_999L
        assertTrue(store.isGranted(AutofillGrantContext("com.example.app", null)))

        now = 30_000L
        assertFalse(store.isGranted(AutofillGrantContext("com.example.app", null)))
    }

    @Test
    fun `clear revokes the grant`() {
        val store = store()
        store.grant(AutofillGrantContext("com.example.app", null))
        store.clear()

        assertFalse(store.isGranted(AutofillGrantContext("com.example.app", null)))
    }

    @Test
    fun `context is normalized before comparison`() {
        val store = store()
        store.grant(AutofillGrantContext("  COM.EXAMPLE.APP ", "https://Accounts.Google.com/"))

        assertTrue(store.isGranted(AutofillGrantContext("com.example.app", "accounts.google.com")))
    }

    @Test
    fun `blank package is not granted`() {
        val store = store()
        store.grant(AutofillGrantContext("   ", "example.com"))

        assertFalse(store.isGranted(AutofillGrantContext("   ", "example.com")))
    }

    @Test
    fun `expired check cannot revoke a concurrent replacement grant`() {
        val expiredCheckEntered = CountDownLatch(1)
        val releaseExpiredCheck = CountDownLatch(1)
        var clockRead = 0
        val store = AutofillSessionGrantStore(ttlMillis = 10L) {
            when (++clockRead) {
                1 -> 0L
                2 -> {
                    expiredCheckEntered.countDown()
                    releaseExpiredCheck.await(5, TimeUnit.SECONDS)
                    10L
                }
                else -> 10L
            }
        }
        val oldContext = AutofillGrantContext("com.example.old", null)
        val newContext = AutofillGrantContext("com.example.new", null)
        store.grant(oldContext)

        var expiredResult = true
        val expiredThread = thread(start = true, name = "expired-grant-check") {
            expiredResult = store.isGranted(oldContext)
        }
        assertTrue(expiredCheckEntered.await(5, TimeUnit.SECONDS))

        val replacementAttempted = CountDownLatch(1)
        val replacementCompleted = CountDownLatch(1)
        val replacementThread = thread(start = true, name = "replacement-grant") {
            replacementAttempted.countDown()
            store.grant(newContext)
            replacementCompleted.countDown()
        }

        try {
            assertTrue(replacementAttempted.await(5, TimeUnit.SECONDS))
            assertFalse(replacementCompleted.await(100, TimeUnit.MILLISECONDS))
        } finally {
            releaseExpiredCheck.countDown()
        }

        expiredThread.join(5_000L)
        replacementThread.join(5_000L)
        assertFalse(expiredThread.isAlive)
        assertFalse(replacementThread.isAlive)
        assertFalse(expiredResult)
        assertTrue(store.isGranted(newContext))
    }
}
