package com.aozijx.passly.security.authentication.host

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestOwnedSlotTest {
    @Test
    fun lateCompletionFromOldOwnerCannotClearNewPrompt() {
        val slot = RequestOwnedSlot<Any>()
        val oldOwner = AuthUiRequestId()
        val oldPrompt = Any()
        val newOwner = AuthUiRequestId()
        val newPrompt = Any()

        assertTrue(slot.install(oldOwner, oldPrompt))
        assertSame(oldPrompt, slot.take(oldOwner))
        assertTrue(slot.install(newOwner, newPrompt))

        assertNull(slot.take(oldOwner))
        assertSame(newPrompt, slot.take(newOwner))
    }

    @Test
    fun activePromptOwnerCannotBeOverwritten() {
        val slot = RequestOwnedSlot<Any>()
        val owner = AuthUiRequestId()
        val prompt = Any()

        assertTrue(slot.install(owner, prompt))
        assertFalse(slot.install(AuthUiRequestId(), Any()))
        assertSame(prompt, slot.take(owner))
    }

    @Test
    fun lifecycleCleanupTakesOnlyTheCurrentPrompt() {
        val slot = RequestOwnedSlot<Any>()
        val owner = AuthUiRequestId()
        val prompt = Any()
        slot.install(owner, prompt)

        assertSame(prompt, slot.takeCurrent())
        assertNull(slot.take(owner))
    }

    @Test
    fun cancellingOwnedPromptAlsoCancelsItsWaitingContinuation() =
        runBlocking<Unit> {
            val slot = RequestOwnedSlot<ActiveBiometricPrompt>()
            val requestId = AuthUiRequestId()
            var promptCancellations = 0
            val request =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    suspendCancellableCoroutine<BiometricHostResult> { continuation ->
                        assertTrue(
                            slot.install(
                                requestId,
                                ActiveBiometricPrompt(continuation) { promptCancellations++ },
                            ),
                        )
                    }
                }

            slot.takeCurrent()?.cancel()
            request.join()

            assertFalse(request.isActive)
            assertEquals(1, promptCancellations)
        }

    @Test
    fun cancelledPromptOwnerCannotStartAuthentication() =
        runBlocking<Unit> {
            var promptStarts = 0
            val request =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    suspendCancellableCoroutine<BiometricHostResult> { continuation ->
                        val prompt = ActiveBiometricPrompt(continuation) {}
                        prompt.cancelPrompt()

                        assertFalse(prompt.startIfActive { promptStarts++ })
                        continuation.cancel()
                    }
                }

            request.join()

            assertEquals(0, promptStarts)
        }

    @Test
    fun cancellationWaitsUntilStartedAuthenticationReturns() =
        runBlocking<Unit> {
            val startEntered = CompletableDeferred<Unit>()
            val cancelAttempted = CompletableDeferred<Unit>()
            val finishStart = CompletableDeferred<Unit>()
            val cancellationFinished = CompletableDeferred<Unit>()
            val events = mutableListOf<String>()
            lateinit var prompt: ActiveBiometricPrompt
            val request =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    suspendCancellableCoroutine<BiometricHostResult> { continuation ->
                        prompt =
                            ActiveBiometricPrompt(continuation) {
                                events += "cancel"
                                cancellationFinished.complete(Unit)
                            }
                    }
                }

            val start =
                async(Dispatchers.Default) {
                    prompt.startIfActive {
                        events += "start"
                        startEntered.complete(Unit)
                        runBlocking { finishStart.await() }
                        events += "start-finished"
                    }
                }
            startEntered.await()
            val cancel =
                async(Dispatchers.Default) {
                    cancelAttempted.complete(Unit)
                    prompt.cancelPrompt()
                }
            cancelAttempted.await()
            yield()
            assertFalse(cancellationFinished.isCompleted)

            finishStart.complete(Unit)
            start.await()
            cancel.await()
            request.cancelAndJoin()

            assertEquals(listOf("start", "start-finished", "cancel"), events)
        }
}
