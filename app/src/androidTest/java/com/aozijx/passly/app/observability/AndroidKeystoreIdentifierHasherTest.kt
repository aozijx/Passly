package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.IdentifierNamespace
import java.security.KeyStore
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AndroidKeystoreIdentifierHasherTest {
    @Test
    fun sameInputIsStableAndNamespaceSeparatedWithoutExposingInput() {
        val alias = "com.aozijx.passly.observability.test.${UUID.randomUUID()}"
        try {
            val hasher = AndroidKeystoreIdentifierHasher(alias)
            val raw = "com.example.bank"

            val targetFirst = hasher.hash(IdentifierNamespace.TARGET_PACKAGE, raw)
            val targetSecond = hasher.hash(IdentifierNamespace.TARGET_PACKAGE, raw)
            val browser = hasher.hash(IdentifierNamespace.BROWSER_PACKAGE, raw)

            assertNotNull(targetFirst)
            assertEquals(targetFirst, targetSecond)
            assertNotEquals(targetFirst, browser)
            assertFalse(targetFirst!!.value.contains(raw))
        } finally {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
        }
    }
}
