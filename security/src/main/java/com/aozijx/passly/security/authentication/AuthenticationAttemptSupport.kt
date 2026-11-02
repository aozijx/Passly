package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationRequestId
import com.aozijx.passly.domain.access.model.AuthenticationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

internal class AuthenticationRequestSlot {
    private val mutex = Mutex()

    suspend fun <T> withSlot(
        onBusy: () -> T,
        block: suspend () -> T,
    ): T {
        if (!mutex.tryLock()) return onBusy()
        return try {
            block()
        } finally {
            mutex.unlock()
        }
    }
}

internal suspend fun <T> withConsumedCredential(
    input: AuthInput,
    block: suspend (CharArray?) -> T,
): T {
    val credential = input.consumeCredential()
    return try {
        block(credential)
    } finally {
        credential?.fill('\u0000')
    }
}

internal suspend fun recoverAuthenticationFailure(
    failure: Throwable,
    requestId: AuthenticationRequestId,
    opensSession: Boolean,
    recordFailure: (Throwable) -> Unit,
    restoreState: suspend () -> Unit,
): AuthenticationResult {
    val secondaryFailures = mutableListOf<Throwable>()
    if (failure !is CancellationException) {
        try {
            recordFailure(failure)
        } catch (secondary: Throwable) {
            secondaryFailures += secondary
        }
    }
    if (opensSession) {
        try {
            if (failure is CancellationException) {
                withContext(NonCancellable) { restoreState() }
            } else {
                restoreState()
            }
        } catch (secondary: Throwable) {
            secondaryFailures += secondary
        }
    }

    if (failure is Error || failure is CancellationException) {
        secondaryFailures.forEach(failure::addSuppressedIfDistinct)
        throw failure
    }
    val higherPriorityFailure = secondaryFailures.firstOrNull { it is Error || it is CancellationException }
    if (higherPriorityFailure != null) {
        secondaryFailures
            .filterNot { it === higherPriorityFailure }
            .forEach(higherPriorityFailure::addSuppressedIfDistinct)
        higherPriorityFailure.addSuppressedIfDistinct(failure)
        throw higherPriorityFailure
    }
    secondaryFailures.forEach(failure::addSuppressedIfDistinct)
    return AuthenticationResult.Failure(
        AuthenticationFailure(
            AuthenticationFailureCode.SESSION_TRANSITION_FAILED,
            requestId,
        ),
    )
}

internal fun Throwable.addSuppressedIfDistinct(secondary: Throwable) {
    if (this !== secondary) addSuppressed(secondary)
}
