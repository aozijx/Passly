package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.policy.WifiQrPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EntryEditorSeedStoreTest {
    private val wifi = EntryEditorSeed.Wifi(
        WifiQrPayload("Office", "secret-password", WifiSecurityType.WpaPersonal, false),
    )

    @Test
    fun `staged seed is consumed exactly once`() {
        val store = EntryEditorSeedStore()
        val token = store.stage(wifi)

        assertEquals(EntryEditorSeedConsumeResult.Consumed(wifi), store.consume(token))
        assertEquals(EntryEditorSeedConsumeResult.Missing, store.consume(token))
    }

    @Test
    fun `staging a new seed invalidates the previous token`() {
        val store = EntryEditorSeedStore()
        val first = store.stage(wifi)
        val second = store.stage(wifi)

        assertNotEquals(first, second)
        assertEquals(EntryEditorSeedConsumeResult.Missing, store.consume(first))
        assertEquals(EntryEditorSeedConsumeResult.Consumed(wifi), store.consume(second))
    }

    @Test
    fun `clear removes staged sensitive content`() {
        val store = EntryEditorSeedStore()
        val token = store.stage(wifi)

        store.clear()

        assertEquals(EntryEditorSeedConsumeResult.Missing, store.consume(token))
    }
}
