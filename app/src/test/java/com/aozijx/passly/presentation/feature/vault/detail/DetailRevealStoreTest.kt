package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.sensitive.OwnedChars
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailRevealStoreTest {

    @Test
    fun `hiding a revealed field wipes its owned value`() {
        val store = DetailRevealStore()
        val secret = OwnedChars.fromString("secret")
        store.replace("password", secret)

        store.replace("password", null)

        assertTrue(secret.isWiped)
    }

    @Test
    fun `replacing and clearing revealed fields wipes every previous owner`() {
        val store = DetailRevealStore()
        val first = OwnedChars.fromString("first")
        val second = OwnedChars.fromString("second")
        store.replace("password", first)

        store.replace("password", second)
        assertTrue(first.isWiped)

        store.clear()
        assertTrue(second.isWiped)
    }
}
