package com.aozijx.passly.data.repository.attachment

import com.aozijx.passly.data.local.database.entity.AttachmentResourceEntity
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentGarbageCollectionWorkerTest {
    @Test
    fun `drain pass pages entire queue and continues after failed item`() =
        runTest {
            val pending = (0 until 130).map(::pendingResource)
            val processed = mutableListOf<String>()
            val batches = mutableListOf<List<String>>()
            val failures = mutableListOf<Exception>()

            val result =
                drainAttachmentGcPass(
                    batchSize = BATCH_SIZE,
                    access = MutableAccess(true),
                    onItemFailure = failures::add,
                    loadBatch = { cursorTime, cursorId ->
                        pending
                            .asSequence()
                            .filter { item ->
                                cursorTime == null ||
                                    item.enqueuedAt!! > cursorTime ||
                                    (item.enqueuedAt == cursorTime && item.resourceId > cursorId!!)
                            }.sortedWith(compareBy(AttachmentResourceEntity::enqueuedAt, AttachmentResourceEntity::resourceId))
                            .take(BATCH_SIZE)
                            .toList()
                            .also { batch -> batches += batch.map(AttachmentResourceEntity::resourceId) }
                    },
                    drainOne = { item ->
                        processed += item.resourceId
                        if (item.resourceId == pending.first().resourceId) {
                            throw IOException("temporary row finalization failure")
                        }
                        true
                    },
                )

            assertEquals(AttachmentGcPassResult.RETRY, result)
            assertEquals(listOf(64, 64, 2), batches.map(List<String>::size))
            assertEquals(130, processed.size)
            assertEquals(130, processed.toSet().size)
            assertEquals(listOf("temporary row finalization failure"), failures.map(Exception::message))
        }

    @Test
    fun `drain pass stops before next database page after authorization is lost`() =
        runTest {
            val pending = (0 until BATCH_SIZE + 1).map(::pendingResource)
            val access = MutableAccess(true)
            var reads = 0

            val result =
                drainAttachmentGcPass(
                    batchSize = BATCH_SIZE,
                    access = access,
                    loadBatch = { _, _ ->
                        reads += 1
                        pending.take(BATCH_SIZE)
                    },
                    drainOne = {
                        access.authorized = false
                        true
                    },
                )

            assertEquals(AttachmentGcPassResult.LOCKED, result)
            assertEquals(1, reads)
        }

    @Test
    fun `retry loop retries database failure with capped backoff`() =
        runTest {
            var attempts = 0
            val delays = mutableListOf<Long>()
            val failures = mutableListOf<Exception>()

            runAttachmentGcDrain(
                access = MutableAccess(true),
                drainPass = {
                    attempts += 1
                    if (attempts < 4) throw IOException("temporary database failure")
                    AttachmentGcPassResult.COMPLETE
                },
                onFailure = failures::add,
                waitBeforeRetry = delays::add,
                initialDelayMs = 5,
                maximumDelayMs = 10,
            )

            assertEquals(4, attempts)
            assertEquals(listOf(5L, 10L, 10L), delays)
            assertEquals(3, failures.size)
        }

    @Test
    fun `retry loop waits after file failure and retries while session remains authorized`() =
        runTest {
            var attempts = 0
            val delays = mutableListOf<Long>()

            runAttachmentGcDrain(
                access = MutableAccess(true),
                drainPass = {
                    attempts += 1
                    if (attempts == 1) AttachmentGcPassResult.RETRY else AttachmentGcPassResult.COMPLETE
                },
                onFailure = {},
                waitBeforeRetry = delays::add,
                initialDelayMs = 5,
                maximumDelayMs = 20,
            )

            assertEquals(2, attempts)
            assertEquals(listOf(5L), delays)
        }

    @Test
    fun `retry loop stops after authorization is lost during backoff`() =
        runTest {
            val access = MutableAccess(true)
            var attempts = 0

            runAttachmentGcDrain(
                access = access,
                drainPass = {
                    attempts += 1
                    AttachmentGcPassResult.RETRY
                },
                onFailure = {},
                waitBeforeRetry = { access.authorized = false },
                initialDelayMs = 5,
                maximumDelayMs = 20,
            )

            assertEquals(1, attempts)
            assertFalse(access.authorized)
        }

    @Test
    fun `fatal drain error propagates without retry`() =
        runTest {
            val fatal = AssertionError("fatal database failure")
            var attempts = 0
            var reported = false

            val actual =
                runCatching {
                    runAttachmentGcDrain(
                        access = MutableAccess(true),
                        drainPass = {
                            attempts += 1
                            throw fatal
                        },
                        onFailure = { reported = true },
                        waitBeforeRetry = {},
                        initialDelayMs = 5,
                        maximumDelayMs = 20,
                    )
                }.exceptionOrNull()

            assertSame(fatal, actual)
            assertEquals(1, attempts)
            assertFalse(reported)
        }

    @Test
    fun `cancellation stops retry loop without reporting or retrying`() =
        runTest {
            val cancelled = CancellationException("worker cancelled")
            var attempts = 0
            var reported = false

            val actual =
                runCatching {
                    runAttachmentGcDrain(
                        access = MutableAccess(true),
                        drainPass = {
                            attempts += 1
                            throw cancelled
                        },
                        onFailure = { reported = true },
                        waitBeforeRetry = {},
                        initialDelayMs = 5,
                        maximumDelayMs = 20,
                    )
                }.exceptionOrNull()

            assertSame(cancelled, actual)
            assertEquals(1, attempts)
            assertFalse(reported)
        }

    private fun pendingResource(index: Int) =
        AttachmentResourceEntity(
            resourceId = index.toString().padStart(3, '0'),
            fileSize = 1,
            lifecycleState = "PENDING_GC",
            createdAt = 1L,
            enqueuedAt = 10L,
        )

    private class MutableAccess(initiallyAuthorized: Boolean) : SecureSessionAccessState {
        private val state =
            MutableStateFlow<AuthenticationState>(
                if (initiallyAuthorized) AuthenticationState.Authenticated(1L) else AuthenticationState.Locked,
            )

        override val authenticationState: StateFlow<AuthenticationState> = state

        var authorized: Boolean
            get() = state.value is AuthenticationState.Authenticated
            set(value) {
                state.value = if (value) AuthenticationState.Authenticated(1L) else AuthenticationState.Locked
            }

        override fun isUnlocked(): Boolean = authorized
    }

    private companion object {
        const val BATCH_SIZE = 64
    }
}
