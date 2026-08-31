package com.aozijx.passly.app.entry.favicon

import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FaviconUrlPolicyTest {

    @Test
    fun validate_acceptsDirectPublicHttpsUrl() {
        val policy = policyWith(publicAddress())

        assertEquals("https://example.com/icon.png", policy.validate(" https://example.com/icon.png ").toString())
    }

    @Test
    fun validate_rejectsNonHttpsCredentialsAndLocalHostNames() {
        val policy = policyWith(publicAddress())
        val rejected = listOf(
            "http://example.com/icon.png",
            "https://user:secret@example.com/icon.png",
            "https://localhost/icon.png",
            "https://device.local/icon.png",
            "https://service.internal/icon.png",
            "https://router.lan/icon.png",
        )

        rejected.forEach { value ->
            assertThrows(FaviconUrlException::class.java) { policy.validate(value) }
        }
    }

    @Test
    fun validate_rejectsIpLiteralsWithoutResolvingThem() {
        var resolverCalled = false
        val policy = FaviconUrlPolicy {
            resolverCalled = true
            arrayOf(publicAddress())
        }

        listOf("https://127.0.0.1/icon.png", "https://[::1]/icon.png").forEach { value ->
            assertThrows(FaviconUrlException::class.java) { policy.validate(value) }
        }
        assertEquals(false, resolverCalled)
    }

    @Test
    fun validate_rejectsEveryNonPublicResolvedAddressClass() {
        val rejectedAddresses = listOf(
            address(0, 0, 0, 0),
            address(127, 0, 0, 1),
            address(169, 254, 1, 1),
            address(10, 0, 0, 1),
            address(100, 64, 0, 1),
            address(224, 0, 0, 1),
            ipv6Address(0xfc),
            ipv6Address(0xfe, 0x80),
            ipv6Address(0xff, 0x02),
        )

        rejectedAddresses.forEach { rejected ->
            val error = assertThrows(FaviconUrlException::class.java) {
                policyWith(rejected).validate("https://example.com/icon.png")
            }
            assertEquals(FaviconUrlFailure.PRIVATE_ADDRESS, error.reason)
        }
    }

    @Test
    fun resolveRedirect_revalidatesResolvedTarget() {
        val policy = FaviconUrlPolicy { host ->
            if (host == "private.example") arrayOf(address(10, 0, 0, 1)) else arrayOf(publicAddress())
        }
        val current = policy.validate("https://public.example/icon.png")

        assertThrows(FaviconUrlException::class.java) {
            policy.resolveRedirect(current, "https://private.example/next.png")
        }
    }

    private fun policyWith(address: InetAddress) = FaviconUrlPolicy { arrayOf(address) }

    private fun publicAddress() = address(93, 184, 216, 34)

    private fun address(a: Int, b: Int, c: Int, d: Int): InetAddress =
        InetAddress.getByAddress(byteArrayOf(a.toByte(), b.toByte(), c.toByte(), d.toByte()))

    private fun ipv6Address(first: Int, second: Int = 0): InetAddress =
        InetAddress.getByAddress(ByteArray(16).also { bytes ->
            bytes[0] = first.toByte()
            bytes[1] = second.toByte()
            bytes[15] = 1
        })
}
