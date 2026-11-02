package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AuthenticationMethodsState {
    private val refreshMutex = Mutex()
    private val stateLock = Any()
    private var stateRevision = 0L
    private var refreshGeneration = 0L
    private val _methods = MutableStateFlow(AuthenticationMethods())

    val methods: StateFlow<AuthenticationMethods> = _methods.asStateFlow()
    val value: AuthenticationMethods get() = _methods.value

    fun beginRefresh(): Long =
        synchronized(stateLock) {
            ++refreshGeneration
        }

    suspend fun refresh(
        generation: Long,
        resolve: suspend () -> AuthenticationMethods,
        onFailure: suspend (Throwable) -> Unit,
    ) {
        refreshMutex.withLock {
            val revision = synchronized(stateLock) { stateRevision }
            val methods =
                try {
                    resolve()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Throwable) {
                    try {
                        onFailure(failure)
                    } catch (observerFailure: Throwable) {
                        when (observerFailure) {
                            is CancellationException,
                            is Error,
                            -> {
                                if (observerFailure !== failure) observerFailure.addSuppressed(failure)
                                throw observerFailure
                            }

                            else -> {
                                if (observerFailure !== failure) failure.addSuppressed(observerFailure)
                            }
                        }
                    }
                    throw failure
                }
            synchronized(stateLock) {
                if (generation == refreshGeneration && revision == stateRevision) {
                    _methods.value = methods
                }
            }
        }
    }

    fun onRecoveryCodeConsumed() =
        synchronized(stateLock) {
            stateRevision++
            _methods.value =
                AuthenticationMethods(
                    _methods.value.available - AuthenticationMethod.RECOVERY_CODE,
                )
        }
}
