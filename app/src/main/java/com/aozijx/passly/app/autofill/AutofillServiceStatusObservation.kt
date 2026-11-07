package com.aozijx.passly.app.autofill

import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun observeBooleanStatus(
    read: () -> Boolean,
    registerObserver: (onChange: () -> Unit) -> (() -> Unit),
): Flow<Boolean> =
    callbackFlow {
        var unregisterObserver: (() -> Unit)? = null
        val sourceFailure = AtomicReference<Throwable?>()

        fun publishStatus() {
            try {
                trySend(read())
            } catch (exception: Exception) {
                sourceFailure.set(exception)
                close(exception)
            } catch (error: Error) {
                sourceFailure.set(error)
                close(error)
                throw error
            }
        }

        try {
            unregisterObserver = registerObserver(::publishStatus)
            publishStatus()
            awaitClose()
        } catch (error: Throwable) {
            sourceFailure.set(error)
            throw error
        } finally {
            try {
                unregisterObserver?.invoke()
            } catch (cleanupFailure: Throwable) {
                val failure = sourceFailure.get()
                if (cleanupFailure is Error) {
                    if (failure != null && cleanupFailure !== failure) cleanupFailure.addSuppressed(failure)
                    throw cleanupFailure
                }
                if (failure == null) throw cleanupFailure
                if (cleanupFailure !== failure) failure.addSuppressed(cleanupFailure)
            }
        }
    }
