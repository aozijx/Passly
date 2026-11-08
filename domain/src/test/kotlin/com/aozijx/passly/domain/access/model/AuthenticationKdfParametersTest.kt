package com.aozijx.passly.domain.access.model

import org.junit.Assert.assertThrows
import org.junit.Test

class AuthenticationKdfParametersTest {
    @Test
    fun `rejects unsupported and out of policy work factors before derivation`() {
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationKdfParameters(0x10, 65_536, 3, 4, 256)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationKdfParameters(0x13, 1_048_576, 3, 4, 256)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationKdfParameters(0x13, 98_304, 3, 4, 256)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationKdfParameters(0x13, 65_536, 2, 4, 256)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationKdfParameters(0x13, 65_536, 5, 4, 256)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationKdfParameters(0x13, 131_072, 4, 4, 256)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationKdfParameters(0x13, 65_536, 3, 2, 256)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationKdfParameters(0x13, 65_536, 3, 4, 128)
        }
    }
}
