package com.aozijx.passly.feature.autofill.legacy.service

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun launchLegacyInteractionExpiry(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    expire: suspend () -> Unit,
): Job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
    withContext(NonCancellable + dispatcher) {
        expire()
    }
}
