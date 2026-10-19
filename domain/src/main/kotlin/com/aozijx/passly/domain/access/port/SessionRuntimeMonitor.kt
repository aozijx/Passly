package com.aozijx.passly.domain.access.port

import kotlinx.coroutines.flow.StateFlow

interface SessionActivityReporter {
    fun onUserInteraction()
}

/** Reports process visibility so the secure session owns its lock timers. */
interface SessionVisibilityController {
    fun onAppForeground()
    fun onAppBackground()
}

interface DatabaseSessionFailureState {
    val databaseFailure: StateFlow<Throwable?>
}
