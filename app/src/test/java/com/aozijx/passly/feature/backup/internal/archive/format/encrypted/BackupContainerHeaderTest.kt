package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

import com.aozijx.passly.core.crypto.KeyDerivation
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupContainerHeaderTest {
    @Test
    fun canonicalHeader_keepsContainerVersionOneAndArgonVersionFromParameters() {
        val salt = ByteArray(16) { it.toByte() }
        val noncePrefix = byteArrayOf(3, 5, 7, 9)
        val parameters =
            KeyDerivation.Argon2idParameters(
                version = KeyDerivation.ARGON2_VERSION_13,
                iterations = 3,
                memoryKiB = 32 * 1024,
                parallelism = 2,
                keyLengthBits = 256,
            )
        val encoded = BackupContainerHeader.encode(salt, noncePrefix, parameters)
        val decoded = BackupContainerHeader.read(ByteArrayInputStream(encoded))

        assertArrayEquals("PSLYBKP1".toByteArray(), encoded.copyOfRange(0, 8))
        assertEquals(1, decoded.containerVersion)
        assertEquals(parameters.version, decoded.kdfParameters.version)
        assertEquals(parameters, decoded.kdfParameters)
        assertArrayEquals(salt, decoded.salt)
        assertArrayEquals(noncePrefix, decoded.noncePrefix)
    }

    @Test
    fun headerRead_rejectsLegacySingleBlockHeaderLength() {
        val oldHeader = ByteArray(88)
        "PSLYBKP1".toByteArray().copyInto(oldHeader)
        oldHeader[11] = 88

        assertThrows(IllegalArgumentException::class.java) {
            BackupContainerHeader.read(ByteArrayInputStream(oldHeader))
        }
    }
}
