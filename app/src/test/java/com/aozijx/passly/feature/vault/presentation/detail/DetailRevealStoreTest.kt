package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.sensitive.OwnedChars
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailRevealStoreTest {
    @Test
    fun `hiding a revealed field wipes its owned value`() {
        val store = DetailRevealStore()
        val secret = OwnedChars.fromString("secret")
        store.add(FieldKey.PASSWORD, secret)

        store.remove(FieldKey.PASSWORD)

        assertTrue(secret.isWiped)
    }

    @Test
    fun `replacing and clearing revealed fields wipes every previous owner`() {
        val store = DetailRevealStore()
        val first = OwnedChars.fromString("first")
        val second = OwnedChars.fromString("second")
        store.add(FieldKey.PASSWORD, first)

        store.add(FieldKey.PASSWORD, second)
        assertTrue(first.isWiped)

        store.clear()
        assertTrue(second.isWiped)
    }
}
