package com.aozijx.passly.data.repository.attachment

import com.aozijx.passly.data.local.database.entity.AttachmentResourceEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

internal enum class AttachmentGcPassResult {
    COMPLETE,
    RETRY,
    LOCKED,
}

/** Processes a stable paged snapshot, continuing past failed rows so they cannot starve later files. */
internal suspend fun drainAttachmentGcPass(
    batchSize: Int,
    hasFullAccess: () -> Boolean,
    onItemFailure: suspend (Exception) -> Unit = {},
    loadBatch: suspend (afterEnqueuedAt: Long?, afterResourceId: String?) -> List<AttachmentResourceEntity>,
    drainOne: suspend (AttachmentResourceEntity) -> Boolean,
): AttachmentGcPassResult {
    require(batchSize > 0)
    var cursorEnqueuedAt: Long? = null
    var cursorResourceId: String? = null
    var hadFailure = false

    while (hasFullAccess()) {
        val batch = loadBatch(cursorEnqueuedAt, cursorResourceId)
        if (batch.isEmpty()) return if (hadFailure) AttachmentGcPassResult.RETRY else AttachmentGcPassResult.COMPLETE

        for (item in batch) {
            if (!hasFullAccess()) return AttachmentGcPassResult.LOCKED
            try {
                if (!drainOne(item)) hadFailure = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                onItemFailure(failure)
                hadFailure = true
            }
            cursorEnqueuedAt = requireNotNull(item.enqueuedAt) { "Pending attachment is missing enqueuedAt" }
            cursorResourceId = item.resourceId
        }

        if (batch.size < batchSize) {
            return if (hadFailure) AttachmentGcPassResult.RETRY else AttachmentGcPassResult.COMPLETE
        }
    }

    return AttachmentGcPassResult.LOCKED
}

/** Retries transient cleanup failures with capped exponential backoff while full vault access remains. */
internal suspend fun runAttachmentGcDrain(
    hasFullAccess: () -> Boolean,
    drainPass: suspend () -> AttachmentGcPassResult,
    onFailure: suspend (Exception) -> Unit,
    waitBeforeRetry: suspend (Long) -> Unit = { delay(it) },
    initialDelayMs: Long = INITIAL_RETRY_DELAY_MS,
    maximumDelayMs: Long = MAXIMUM_RETRY_DELAY_MS,
) {
    require(initialDelayMs > 0 && maximumDelayMs >= initialDelayMs)
    var retryDelayMs = initialDelayMs

    while (hasFullAccess()) {
        val result =
            try {
                drainPass()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                onFailure(failure)
                AttachmentGcPassResult.RETRY
            }

        when (result) {
            AttachmentGcPassResult.COMPLETE,
            AttachmentGcPassResult.LOCKED,
            -> {
                return
            }

            AttachmentGcPassResult.RETRY -> {
                if (!hasFullAccess()) return
                waitBeforeRetry(retryDelayMs)
                retryDelayMs = (retryDelayMs * 2).coerceAtMost(maximumDelayMs)
            }
        }
    }
}

internal const val INITIAL_RETRY_DELAY_MS = 250L
internal const val MAXIMUM_RETRY_DELAY_MS = 30_000L
