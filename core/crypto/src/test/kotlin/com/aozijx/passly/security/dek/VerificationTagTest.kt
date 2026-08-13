package com.aozijx.passly.security.dek

import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.SecureRandom

class VerificationTagTest {

    @Test
    fun verify_acceptsCreatingDek() {
        val dek = randomDek()
        val tag = VerificationTag.create(dek)

        VerificationTag.verify(dek, tag, "test")
    }

    @Test
    fun verify_rejectsDifferentDek() {
        val tag = VerificationTag.create(randomDek())

        assertThrows(Exception::class.java) {
            VerificationTag.verify(randomDek(), tag, "test")
        }
    }

    @Test
    fun verify_rejectsTruncatedTag() {
        assertThrows(IllegalArgumentException::class.java) {
            VerificationTag.verify(randomDek(), byteArrayOf(1, 2, 3), "test")
        }
    }

    private fun randomDek(): ByteArray =
        ByteArray(32).also(SecureRandom()::nextBytes)
}
