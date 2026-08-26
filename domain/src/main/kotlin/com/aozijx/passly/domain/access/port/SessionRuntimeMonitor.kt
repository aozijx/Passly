package com.aozijx.passly.domain.access.port

import kotlinx.coroutines.flow.StateFlow

interface SessionActivityReporter {
    fun onUserInteraction()
}

interface DatabaseSessionFailureState {
    val databaseFailure: StateFlow<Throwable?>
    fun clearDatabaseFailure()
}
