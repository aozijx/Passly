package com.aozijx.passly.app

import android.view.View

private const val APP_CLOSE_MESSAGE_DELAY_MS = 1_000L

internal interface TaskClearCallbackScheduler {
    fun post(callback: Runnable)

    fun postDelayed(
        callback: Runnable,
        delayMs: Long,
    )

    fun remove(callback: Runnable)
}

internal class ViewTaskClearCallbackScheduler(
    private val view: View,
) : TaskClearCallbackScheduler {
    override fun post(callback: Runnable) {
        view.post(callback)
    }

    override fun postDelayed(
        callback: Runnable,
        delayMs: Long,
    ) {
        view.postDelayed(callback, delayMs)
    }

    override fun remove(callback: Runnable) {
        view.removeCallbacks(callback)
    }
}

/** Binds one task-clear claim and its reminder delay to an Activity's resumed lifecycle. */
internal class TaskClearActivityOwner(
    private val requests: ActiveTaskClearRequests,
    private val scheduler: TaskClearCallbackScheduler,
    private val showReminder: () -> Unit,
    private val closeTask: () -> Unit,
    private val ownerId: TaskClearOwnerId = TaskClearOwnerId(),
) {
    private var resumed = false
    private var activeClaim: TaskClearClaim? = null
    private var deliveryCallback: Runnable? = null
    private var delayedCloseCallback: Runnable? = null

    fun onResumed() {
        resumed = true
        requests.registerOwner(ownerId, ::onRequest, ::onRequestCancelled)
    }

    fun onPaused() {
        resumed = false
        cancelCallbacks()
        requests.releaseOwner(ownerId)
    }

    private fun onRequest(claim: TaskClearClaim) {
        if (activeClaim == claim && (deliveryCallback != null || delayedCloseCallback != null)) return
        activeClaim = claim
        val delivery =
            Runnable {
                if (activeClaim != claim || !resumed) return@Runnable
                deliveryCallback = null
                if (!requests.isCurrent(claim)) return@Runnable
                showReminder()
                val delayedClose =
                    Runnable {
                        if (activeClaim != claim) return@Runnable
                        activeClaim = null
                        delayedCloseCallback = null
                        if (resumed && requests.consume(claim)) closeTask()
                    }
                delayedCloseCallback = delayedClose
                scheduler.postDelayed(delayedClose, APP_CLOSE_MESSAGE_DELAY_MS)
            }
        deliveryCallback = delivery
        scheduler.post(delivery)
    }

    private fun onRequestCancelled(requestId: TaskClearRequestId) {
        scheduler.post {
            if (activeClaim?.requestId == requestId) cancelCallbacks()
        }
    }

    private fun cancelCallbacks() {
        deliveryCallback?.let(scheduler::remove)
        deliveryCallback = null
        delayedCloseCallback?.let(scheduler::remove)
        delayedCloseCallback = null
        activeClaim = null
    }
}
