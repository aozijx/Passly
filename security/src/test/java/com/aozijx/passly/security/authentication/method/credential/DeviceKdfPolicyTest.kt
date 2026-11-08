package com.aozijx.passly.security.authentication.method.credential
import com.aozijx.passly.core.crypto.KeyDerivation
import com.aozijx.passly.domain.access.model.AuthenticationKdfParameters
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceKdfPolicyTest {
    @Test
    fun `slow or small devices retain the established baseline`() {
        val selected = DeviceKdfPolicy.select(heapMiB = 192, processors = 2, baselineMillis = 650)

        assertEquals(
            AuthenticationKdfParameters(
                version = KeyDerivation.ARGON2_VERSION_13,
                memoryKiB = 65_536,
                iterations = 3,
                parallelism = 4,
                keyLengthBits = 256,
            ),
            selected,
        )
    }

    @Test
    fun `fast devices can increase work without changing parallelism`() {
        val selected = DeviceKdfPolicy.select(heapMiB = 512, processors = 8, baselineMillis = 120)

        assertEquals(
            AuthenticationKdfParameters(
                version = KeyDerivation.ARGON2_VERSION_13,
                memoryKiB = 131_072,
                iterations = 3,
                parallelism = 4,
                keyLengthBits = 256,
            ),
            selected,
        )
    }

    @Test
    fun `moderate baseline increases iterations only through the configured threshold`() {
        val atThreshold = DeviceKdfPolicy.select(heapMiB = 512, processors = 2, baselineMillis = 250)
        val aboveThreshold = DeviceKdfPolicy.select(heapMiB = 512, processors = 2, baselineMillis = 251)

        assertEquals(
            AuthenticationKdfParameters(
                version = KeyDerivation.ARGON2_VERSION_13,
                memoryKiB = 65_536,
                iterations = 4,
                parallelism = 4,
                keyLengthBits = 256,
            ),
            atThreshold,
        )
        assertEquals(
            AuthenticationKdfParameters(
                version = KeyDerivation.ARGON2_VERSION_13,
                memoryKiB = 65_536,
                iterations = 3,
                parallelism = 4,
                keyLengthBits = 256,
            ),
            aboveThreshold,
        )
    }
}
