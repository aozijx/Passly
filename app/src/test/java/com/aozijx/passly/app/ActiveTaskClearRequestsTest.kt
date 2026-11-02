package com.aozijx.passly.app

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveTaskClearRequestsTest {
    @Test
    fun `request made before an owner resumes is claimed once`() {
        val session = TestSessionAccessState(AuthenticationState.Locked)
        val requests = ActiveTaskClearRequests(session)
        val requestId = requests.requestClear()
        val received = mutableListOf<TaskClearClaim>()
        val ownerId = TaskClearOwnerId()

        requests.registerOwner(ownerId, received::add, {})

        assertNotNull(requestId)
        assertEquals(1, received.size)
        assertEquals(requestId, received.single().requestId)
        assertTrue(requests.consume(received.single()))
        assertFalse(requests.consume(received.single()))
    }

    @Test
    fun `released claim remains pending for replacement owner`() {
        val requests = ActiveTaskClearRequests(TestSessionAccessState(AuthenticationState.Locked))
        val requestId = requests.requestClear()
        val firstClaims = mutableListOf<TaskClearClaim>()
        val secondClaims = mutableListOf<TaskClearClaim>()
        val firstOwner = TaskClearOwnerId()
        val secondOwner = TaskClearOwnerId()
        requests.registerOwner(firstOwner, firstClaims::add, {})

        requests.releaseOwner(firstOwner)
        requests.registerOwner(secondOwner, secondClaims::add, {})

        assertEquals(requestId, secondClaims.single().requestId)
        assertFalse(requests.consume(firstClaims.single()))
        assertTrue(requests.consume(secondClaims.single()))
    }

    @Test
    fun `new owner invalidates previous claim and receives the same pending request`() {
        val requests = ActiveTaskClearRequests(TestSessionAccessState(AuthenticationState.Locked))
        val requestId = requests.requestClear()
        val firstClaims = mutableListOf<TaskClearClaim>()
        val secondClaims = mutableListOf<TaskClearClaim>()
        val cancelled = mutableListOf<TaskClearRequestId>()
        requests.registerOwner(TaskClearOwnerId(), firstClaims::add, cancelled::add)

        requests.registerOwner(TaskClearOwnerId(), secondClaims::add, {})

        assertEquals(listOf(requestId), cancelled)
        assertEquals(requestId, secondClaims.single().requestId)
        assertFalse(requests.consume(firstClaims.single()))
        assertTrue(requests.consume(secondClaims.single()))
    }

    @Test
    fun `successful authentication cancels request and notifies owner`() {
        val session = TestSessionAccessState(AuthenticationState.Locked)
        val requests = ActiveTaskClearRequests(session)
        val requestId = requests.requestClear()
        val claims = mutableListOf<TaskClearClaim>()
        val cancelled = mutableListOf<TaskClearRequestId>()
        requests.registerOwner(TaskClearOwnerId(), claims::add, cancelled::add)

        val authenticated = AuthenticationState.Authenticated(1L)
        session.authenticationState.value = authenticated
        requests.onAuthenticationStateChanged(authenticated)

        assertEquals(listOf(requestId), cancelled)
        assertFalse(requests.consume(claims.single()))
        assertNull(requests.requestClear())
    }

    @Test
    fun `request is rejected unless session is locked`() {
        val requests = ActiveTaskClearRequests(TestSessionAccessState(AuthenticationState.Authenticated(1L)))

        assertNull(requests.requestClear())
    }

    private class TestSessionAccessState(
        initial: AuthenticationState,
    ) : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow(initial)

        override fun isUnlocked(): Boolean = authenticationState.value is AuthenticationState.Authenticated
    }
}
