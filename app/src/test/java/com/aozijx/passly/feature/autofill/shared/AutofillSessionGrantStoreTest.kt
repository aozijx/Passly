package com.aozijx.passly.feature.autofill.shared

import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
