package com.aozijx.passly.feature.autofill.runtime

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Start service-owned interaction cleanup immediately and finish it if the service is destroyed. */
internal fun launchAutofillServiceCleanup(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    cleanup: suspend () -> Unit,
    afterCleanup: () -> Unit = {},
): Job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
    withContext(NonCancellable + dispatcher) { cleanup() }
    currentCoroutineContext().ensureActive()
    afterCleanup()
}
