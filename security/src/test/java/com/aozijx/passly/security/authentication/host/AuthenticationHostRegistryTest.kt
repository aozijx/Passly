package com.aozijx.passly.security.authentication.host

import androidx.biometric.BiometricPrompt
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationHostRegistryTest {
    @Test
    fun onlyUsableWeakHostCanBeLeased() = runBlocking<Unit> {
        val registry = AuthenticationHostRegistry()
        val host = FakeHost("autofill", usable = true)
        val token = registry.register(host)

        val lease = checkNotNull(registry.awaitLease(50))
        assertEquals("autofill", lease.ownerId)
        assertEquals(host, lease.hostOrNull())
        assertTrue(registry.isCurrent(lease))
        lease.release()
        assertFalse(registry.isCurrent(lease))

        registry.unregister(token)
        assertNull(registry.awaitLease(10))
    }

    @Test
    fun concurrentLeaseWaitsUntilCurrentUiOperationReleases() = runBlocking<Unit> {
        val registry = AuthenticationHostRegistry()
        val host = FakeHost("authentication", usable = true)
        registry.register(host)
        val firstLease = checkNotNull(registry.awaitLease(50))

        val waitingLease = async(start = CoroutineStart.UNDISPATCHED) {
            registry.awaitLease(50)
        }
        yield()
        assertFalse(waitingLease.isCompleted)

        firstLease.release()
        firstLease.release()
        val secondLease = waitingLease.await()

        assertEquals(host, secondLease?.hostOrNull())
        secondLease?.release()
    }

    @Test
    fun destroyedHostIsNeverReturned() = runBlocking<Unit> {
        val registry = AuthenticationHostRegistry()
        val host = FakeHost("credential", usable = false)
        registry.register(host)

        assertNull(registry.awaitLease(10))

        val usableHost = FakeHost("credential", usable = true)
        registry.register(usableHost)
        val lease = checkNotNull(registry.awaitLease(50))
        assertEquals(usableHost, lease.hostOrNull())
        lease.release()
    }

    @Test
    fun cancellingWaitForUiLeaseDoesNotBlockLaterRequest() = runBlocking<Unit> {
        val registry = AuthenticationHostRegistry()
        val host = FakeHost("authentication", usable = true)
        registry.register(host)
        val activeLease = checkNotNull(registry.awaitLease(50))
        val waitingRequest = async(start = CoroutineStart.UNDISPATCHED) {
            registry.awaitLease(50)
        }

        assertFalse(waitingRequest.isCompleted)
        waitingRequest.cancelAndJoin()
        activeLease.release()

        val nextLease = registry.awaitLease(50)
        assertEquals(host, nextLease?.hostOrNull())
        nextLease?.release()
    }

    @Test
    fun staleRegistrationTokenCannotUnregisterReplacementHost() = runBlocking<Unit> {
        val registry = AuthenticationHostRegistry()
        val staleToken = registry.register(FakeHost("activity-old", usable = true))
        val replacement = FakeHost("activity-new", usable = true)
        val replacementToken = registry.register(replacement)

        registry.unregister(staleToken)

        val lease = registry.awaitLease(50)
        assertEquals(replacement, lease?.hostOrNull())
        assertEquals(replacementToken, lease?.token)
        lease?.release()
    }

    @Test
    fun invalidatingOwnerDoesNotClearAnotherActivityOwner() = runBlocking<Unit> {
        val registry = AuthenticationHostRegistry()
        val replacement = FakeHost("activity-new", usable = true)
        registry.register(FakeHost("activity-old", usable = true))
        registry.register(replacement)

        registry.invalidateOwner("activity-old")

        val lease = registry.awaitLease(50)
        assertEquals(replacement, lease?.hostOrNull())
        lease?.release()
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
