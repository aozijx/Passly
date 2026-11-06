package com.aozijx.passly.domain.access.model

import org.junit.Assert.assertThrows
import org.junit.Test

class AuthenticationKdfParametersTest {
    @Test
    fun `rejects missing and excessive work factors before derivation`() {
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationKdfParameters(0, 0, 0, 0, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationKdfParameters(0x13, 1_048_576, 3, 4, 256)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationKdfParameters(0x13, 98_304, 3, 4, 256)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationKdfParameters(0x13, 65_536, 100, 4, 256)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationKdfParameters(0x13, 65_536, 5, 4, 256)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationKdfParameters(0x13, 131_072, 4, 4, 256)
        }
    }
}
