package com.aozijx.passly.security.authentication

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SingleThreadKdfRunnerTest {
    @Test
    fun cancellingCallerDoesNotWaitForNativeWorkAndLateResultIsDiscarded() = runBlocking {
        val runner = SingleThreadKdfRunner()
        val started = CountDownLatch(1)
        val releaseWorker = CountDownLatch(1)
        val resultDiscarded = CountDownLatch(1)
        val callbackReached = AtomicBoolean(false)
        val secret = SecretChars.copyOf(charArrayOf('s', 'e', 'c', 'r', 'e', 't'))

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            runner.execute(secret) {
                started.countDown()
                releaseWorker.await()
                object : DiscardableResult {
                    override fun discard() {
                        resultDiscarded.countDown()
                    }
                }
            }
            callbackReached.set(true)
        }

        assertTrue(started.await(1, TimeUnit.SECONDS))
        job.cancelAndJoin()
        assertFalse(callbackReached.get())
        assertFalse(resultDiscarded.await(50, TimeUnit.MILLISECONDS))

        releaseWorker.countDown()
        assertTrue(resultDiscarded.await(1, TimeUnit.SECONDS))
        secret.close()
        runner.close()
    }
}
