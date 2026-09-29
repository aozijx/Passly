package com.aozijx.passly.presentation.feature.vault.detail.history

import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailRevisionRevealStoreTest {
    @Test
    fun `clear wipes every owned historical value`() {
        val value = RecordingSensitiveValue()
        val store = DetailRevisionRevealStore()
        store.replace(SensitiveFieldKey.PASSWORD, value)

        store.clear()

        assertTrue(value.wiped)
        assertTrue(store.snapshot().isEmpty())
    }

    private class RecordingSensitiveValue : SensitiveValue {
        var wiped = false
        override val isEmpty: Boolean = false
        override fun toCharArray() = charArrayOf('x')
        override fun wipe() { wiped = true }
    }
}
