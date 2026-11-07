package com.aozijx.passly.security.authentication.method.credential
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceKdfPolicyTest {
    @Test
    fun `slow or small devices retain the established baseline`() {
        val selected = DeviceKdfPolicy.select(heapMiB = 192, processors = 2, baselineMillis = 650)

        assertEquals(65_536, selected.memoryKiB)
        assertEquals(3, selected.iterations)
        assertEquals(4, selected.parallelism)
    }

    @Test
    fun `fast devices can increase work without changing parallelism`() {
        val selected = DeviceKdfPolicy.select(heapMiB = 512, processors = 8, baselineMillis = 120)

        assertEquals(131_072, selected.memoryKiB)
        assertEquals(3, selected.iterations)
        assertEquals(4, selected.parallelism)
    }
}
