package com.aozijx.passly.security.authentication.host

import androidx.biometric.BiometricPrompt
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthenticationHostRegistryTest {
    @Test
    fun onlyUsableWeakHostCanBeLeased() = runBlocking {
        val registry = AuthenticationHostRegistry()
        val host = FakeHost("autofill", usable = true)
        val token = registry.register(host)

        val lease = registry.awaitLease(50)
        assertNotNull(lease)
        assertEquals("autofill", lease?.ownerId)
        assertEquals(host, lease?.hostOrNull())

        registry.unregister(token)
        assertNull(registry.awaitLease(10))
    }

    @Test
    fun destroyedHostIsNeverReturned() = runBlocking {
        val registry = AuthenticationHostRegistry()
        val host = FakeHost("credential", usable = false)
        registry.register(host)

        assertNull(registry.awaitLease(10))
    }

    private class FakeHost(
        override val ownerId: String,
        private val usable: Boolean
    ) : AuthUiHost {
        override fun snapshot() = AuthHostSnapshot(usable, false, !usable)

        override suspend fun chooseMethod(
            purpose: AuthenticationPurpose,
            methods: List<AuthenticationMethod>
        ): AuthenticationMethod? = methods.firstOrNull()

        override suspend fun requestSecret(
            purpose: AuthenticationPurpose,
            method: AuthenticationMethod
        ) = SecretHostResult.Cancelled(true)

        override suspend fun authenticateBiometric(
            spec: BiometricPromptSpec,
            cryptoObject: BiometricPrompt.CryptoObject?
        ) = BiometricHostResult.Cancelled(true)
    }
}
