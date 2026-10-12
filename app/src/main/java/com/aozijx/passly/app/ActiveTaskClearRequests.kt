package com.aozijx.passly.app

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Delivers task-clear commands only to the activity that is currently collecting. */
internal class ActiveTaskClearRequests {
    private val mutableEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val events: SharedFlow<Unit> = mutableEvents.asSharedFlow()

    fun requestClear() {
        mutableEvents.tryEmit(Unit)
    }
}
