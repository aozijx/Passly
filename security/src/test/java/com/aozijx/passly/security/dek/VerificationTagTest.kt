package com.aozijx.passly.security.dek

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertThrows
import org.junit.Test

class VerificationTagTest {
    @Test
    fun verify_acceptsCreatingDek() {
        val dek = randomDek()
        var tag: ByteArray? = null
        try {
            val createdTag = VerificationTag.create(dek)
            tag = createdTag

            VerificationTag.verify(dek, createdTag, "test")
        } finally {
            dek.fill(0)
            tag?.fill(0)
        }
    }

    @Test
    fun verify_rejectsDifferentDek() {
        val correctDek = randomDek()
        val wrongDek = randomDek()
        var tag: ByteArray? = null
        try {
            val createdTag = VerificationTag.create(correctDek)
            tag = createdTag

            assertThrows(AEADBadTagException::class.java) {
                VerificationTag.verify(wrongDek, createdTag, "test")
            }
        } finally {
            correctDek.fill(0)
            wrongDek.fill(0)
            tag?.fill(0)
        }
    }

    @Test
    fun verify_rejectsTruncatedTag() {
        val dek = randomDek()
        try {
            assertThrows(IllegalArgumentException::class.java) {
                VerificationTag.verify(dek, byteArrayOf(1, 2, 3), "test")
            }
        } finally {
            dek.fill(0)
        }
    }

    private fun randomDek(): ByteArray = ByteArray(32).also(SecureRandom()::nextBytes)
}
