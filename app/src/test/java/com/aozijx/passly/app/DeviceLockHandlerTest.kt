package com.aozijx.passly.app

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.LockTrigger
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.settings.model.SecuritySettings
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.testing.RecordingTestLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLockHandlerTest {

    @Test
    fun `continuous face-down sensor samples dispatch one flip until device is released`() {
        val gate = FlipTriggerGate()

        assertTrue(gate.shouldTrigger(-9.0f))
        assertFalse(gate.shouldTrigger(-9.3f))
        assertFalse(gate.shouldTrigger(-8.8f))
        assertFalse(gate.shouldTrigger(-6.5f))
        assertTrue(gate.shouldTrigger(-9.1f))
    }

    @Test
    fun `skipped lock result is not reported as completed`() {
        val logger = RecordingTestLogger()

        logger.log(DeviceLockResult.Locked(shouldClearTask = true).toLogStatement(DeviceLockTrigger.FLIP))
        logger.log(DeviceLockResult.Skipped.toLogStatement(DeviceLockTrigger.FLIP))

        assertEquals(
            listOf(Events.Application.DeviceLockCompleted, Events.Application.DeviceLockSkipped),
            logger.events.map { it.event },
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `task clear request is delivered only to an active task collector`() = runTest {
        val requests = ActiveTaskClearRequests()
        val received = mutableListOf<Unit>()

        requests.requestClear()
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            requests.events.collect { received += it }
        }
        runCurrent()

        assertTrue(received.isEmpty())

        requests.requestClear()
        runCurrent()

        assertEquals(1, received.size)
        collection.cancel()
    }

    @Test
    fun `screen off remains pending until its lock operation completes`() {
        val tracker = ScreenOffLockTracker()

        val generation = tracker.markScreenOff()

        assertEquals(generation, tracker.pendingGeneration())

        tracker.complete(generation)

        assertNull(tracker.pendingGeneration())
    }

    @Test
    fun `older lock completion cannot clear a newer screen off event`() {
        val tracker = ScreenOffLockTracker()
        val olderGeneration = tracker.markScreenOff()
        val newerGeneration = tracker.markScreenOff()

        tracker.complete(olderGeneration)

        assertEquals(newerGeneration, tracker.pendingGeneration())
    }

    @Test
    fun `completed generation is never reused by a later screen off event`() {
        val tracker = ScreenOffLockTracker()
        val completedGeneration = tracker.markScreenOff()
        tracker.complete(completedGeneration)
        val laterGeneration = tracker.markScreenOff()

        tracker.complete(completedGeneration)

        assertEquals(laterGeneration, tracker.pendingGeneration())
    }

    @Test
    fun `screen off seals an unlocked session independently of flip settings`() = runTest {
        val lockController = RecordingLockController()
        val handler = handler(
            lockController = lockController,
            security = SecuritySettings(
                isFlipToLockEnabled = false,
                isFlipExitAndClearStackEnabled = true,
            ),
        )

        val result = handler.handle(DeviceLockTrigger.SCREEN_OFF) as DeviceLockResult.Locked

        assertFalse(result.shouldClearTask)
        assertEquals(LockReason.BACKGROUND, lockController.reason)
        assertEquals(LockTrigger.SCREEN_OFF, lockController.trigger)
    }

    @Test
    fun `screen off follows enabled clear task setting`() = runTest {
        val handler = handler(
            lockController = RecordingLockController(),
            security = SecuritySettings(
                isFlipToLockEnabled = true,
                isFlipExitAndClearStackEnabled = true,
            ),
        )

        val result = handler.handle(DeviceLockTrigger.SCREEN_OFF) as DeviceLockResult.Locked

        assertTrue(result.shouldClearTask)
    }

    @Test
    fun `screen off reconciles the session even when the access snapshot already looks locked`() = runTest {
        val lockController = RecordingLockController()
        val handler = handler(
            lockController = lockController,
            security = SecuritySettings(),
            initialState = AuthenticationState.Locked,
        )

        val result = handler.handle(DeviceLockTrigger.SCREEN_OFF)

        assertTrue(result is DeviceLockResult.Locked)
        assertEquals(LockReason.BACKGROUND, lockController.reason)
        assertEquals(LockTrigger.SCREEN_OFF, lockController.trigger)
    }

    @Test
    fun `disabled flip trigger does not lock the session`() = runTest {
        val lockController = RecordingLockController()
        val handler = handler(
            lockController = lockController,
            security = SecuritySettings(isFlipToLockEnabled = false),
        )

        val result = handler.handle(DeviceLockTrigger.FLIP)

        assertEquals(DeviceLockResult.Skipped, result)
        assertNull(lockController.reason)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `lock waits for the persisted security snapshot`() = runTest {
        val lockController = RecordingLockController()
        val settings = MutableStateFlow<SecuritySettings?>(null)
        val handler = DeviceLockHandler(
            sessionAccessState = FakeSessionState(AuthenticationState.Authenticated(1L)),
            sessionLockController = lockController,
            securitySettings = settings,
        )

        val result = async { handler.handle(DeviceLockTrigger.FLIP) }
        runCurrent()
        assertNull(lockController.reason)

        settings.value = SecuritySettings(isFlipToLockEnabled = true)

        assertTrue(result.await() is DeviceLockResult.Locked)
        assertEquals(LockTrigger.DEVICE_FLIP, lockController.trigger)
    }

    @Test
    fun `enabled flip locks softly and follows clear task setting`() = runTest {
        val lockController = RecordingLockController()
        val handler = handler(
            lockController = lockController,
            security = SecuritySettings(
                isFlipToLockEnabled = true,
                isFlipExitAndClearStackEnabled = true,
            ),
        )

        val result = handler.handle(DeviceLockTrigger.FLIP) as DeviceLockResult.Locked

        assertTrue(result.shouldClearTask)
        assertEquals(LockReason.USER, lockController.reason)
        assertEquals(LockTrigger.DEVICE_FLIP, lockController.trigger)
    }

    private fun handler(
        lockController: SessionLockController,
        security: SecuritySettings,
        initialState: AuthenticationState = AuthenticationState.Authenticated(1L),
    ) = DeviceLockHandler(
        sessionAccessState = FakeSessionState(initialState),
        sessionLockController = lockController,
        securitySettings = MutableStateFlow<SecuritySettings?>(security),
    )

    private class FakeSessionState(initial: AuthenticationState) : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow(initial)
        override fun isUnlocked(): Boolean =
            authenticationState.value is AuthenticationState.Authenticated ||
                authenticationState.value is AuthenticationState.RecoveryMode
    }

    private class RecordingLockController : SessionLockController {
        var reason: LockReason? = null
        var trigger: LockTrigger? = null

        override suspend fun lock(request: SessionLockRequest) {
            reason = request.reason
            trigger = request.trigger
        }
    }
}
