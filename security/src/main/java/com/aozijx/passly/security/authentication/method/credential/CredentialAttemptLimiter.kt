package com.aozijx.passly.security.authentication.method.credential

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.aozijx.passly.domain.access.model.AuthenticationAttemptState
import com.aozijx.passly.domain.access.model.AuthenticationAttemptStatus
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AttemptStatus
import com.aozijx.passly.domain.access.port.AuthenticationAttemptStatusProvider
import com.aozijx.passly.domain.access.port.AuthenticationAttemptStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

private const val MAX_ATTEMPTS = 5
private const val LOCKOUT_MS = 30_000L

internal data class CredentialAttemptTime(
    val epochMs: Long,
    val elapsedRealtimeMs: Long,
    val bootCount: Int,
)

@Singleton
internal class CredentialAttemptLimiter internal constructor(
    private val store: AuthenticationAttemptStore,
    private val clock: () -> CredentialAttemptTime,
) : AuthenticationAttemptStatusProvider {
    @Inject
    constructor(
        store: AuthenticationAttemptStore,
        @ApplicationContext context: Context,
    ) : this(store, { context.currentAttemptTime() })

    suspend fun beforeAttempt(
        method: AuthenticationMethod,
        request: AuthenticationRequest,
    ): AuthenticationFailure? {
        val now =
            try {
                clock()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return unavailableFailure(method, request)
            }
        val state =
            try {
                store.update(method) { it.normalize(now) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return unavailableFailure(method, request)
            }
        return state.rateLimitedFailure(method, request, now)
    }

    suspend fun reserveAttempt(
        method: AuthenticationMethod,
        request: AuthenticationRequest,
    ): AuthenticationFailure? {
        val now =
            try {
                clock()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return unavailableFailure(method, request)
            }
        val state =
            try {
                store.update(method) { current ->
                    val normalized = current.normalize(now)
                    if (normalized.retryAfterMs(now) != null) {
                        normalized
                    } else {
                        normalized.copy(failedAttempts = normalized.failedAttempts + 1)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return unavailableFailure(method, request)
            }
        return state.rateLimitedFailure(method, request, now)
    }

    suspend fun recordIncorrect(
        method: AuthenticationMethod,
        request: AuthenticationRequest,
    ): AuthenticationFailure {
        val now =
            try {
                clock()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return unavailableFailure(method, request)
            }
        val state =
            try {
                store.update(method) { current ->
                    val normalized = current.normalize(now)
                    if (normalized.retryAfterMs(now) != null) {
                        normalized
                    } else if (normalized.failedAttempts >= MAX_ATTEMPTS) {
                        normalized.lockUntil(now)
                    } else {
                        normalized
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return unavailableFailure(method, request)
            }
        return state.rateLimitedFailure(method, request, now)
            ?: AuthenticationFailure(
                code = AuthenticationFailureCode.CREDENTIAL_INCORRECT,
                requestId = request.id,
                method = method,
                attempts = AttemptStatus(used = state.failedAttempts, limit = MAX_ATTEMPTS),
            )
    }

    suspend fun releaseAttempt(
        method: AuthenticationMethod,
        request: AuthenticationRequest,
    ): AuthenticationFailure? =
        try {
            store.update(method) { current ->
                if (current.lockedUntilEpochMs > 0L || current.lockedUntilElapsedRealtimeMs > 0L) {
                    current
                } else {
                    current.copy(failedAttempts = (current.failedAttempts - 1).coerceAtLeast(0))
                }
            }
            null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            unavailableFailure(method, request)
        }

    suspend fun recordSuccess(
        method: AuthenticationMethod,
        request: AuthenticationRequest,
    ): AuthenticationFailure? =
        try {
            store.update(method) { AuthenticationAttemptState() }
            null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            unavailableFailure(method, request)
        }

    override suspend fun status(method: AuthenticationMethod): AuthenticationAttemptStatus {
        val now = clock()
        val state = store.update(method) { it.normalize(now) }
        return AuthenticationAttemptStatus(
            attempts = AttemptStatus(used = state.failedAttempts, limit = MAX_ATTEMPTS),
            retryAfterMs = state.retryAfterMs(now),
        )
    }

    private fun AuthenticationAttemptState.normalize(now: CredentialAttemptTime): AuthenticationAttemptState {
        if (failedAttempts >= MAX_ATTEMPTS && lockedUntilEpochMs <= 0L && lockedUntilElapsedRealtimeMs <= 0L) {
            return lockUntil(now)
        }
        return if (retryAfterMs(now) == null && (lockedUntilEpochMs > 0L || lockedUntilElapsedRealtimeMs > 0L)) {
            AuthenticationAttemptState()
        } else {
            this
        }
    }

    private fun AuthenticationAttemptState.lockUntil(now: CredentialAttemptTime) =
        copy(
            failedAttempts = MAX_ATTEMPTS,
            lockedUntilEpochMs = now.epochMs + LOCKOUT_MS,
            lockedUntilElapsedRealtimeMs = now.elapsedRealtimeMs + LOCKOUT_MS,
            bootCount = now.bootCount,
        )

    private fun AuthenticationAttemptState.retryAfterMs(now: CredentialAttemptTime): Long? {
        val remaining =
            if (bootCount >= 0 && bootCount == now.bootCount && lockedUntilElapsedRealtimeMs > 0L) {
                lockedUntilElapsedRealtimeMs - now.elapsedRealtimeMs
            } else {
                lockedUntilEpochMs - now.epochMs
            }
        return remaining.takeIf { it > 0L }
    }

    private fun AuthenticationAttemptState.rateLimitedFailure(
        method: AuthenticationMethod,
        request: AuthenticationRequest,
        now: CredentialAttemptTime,
    ): AuthenticationFailure? {
        val retryAfter = retryAfterMs(now) ?: return null
        return AuthenticationFailure(
            code = AuthenticationFailureCode.RATE_LIMITED,
            requestId = request.id,
            method = method,
            attempts = AttemptStatus(used = failedAttempts, limit = MAX_ATTEMPTS),
            retryAfterMs = retryAfter,
        )
    }

    private fun unavailableFailure(
        method: AuthenticationMethod,
        request: AuthenticationRequest,
    ) =
        AuthenticationFailure(
            code = AuthenticationFailureCode.ATTEMPT_STATE_UNAVAILABLE,
            requestId = request.id,
            method = method,
        )
}

private fun Context.currentAttemptTime() =
    CredentialAttemptTime(
        epochMs = System.currentTimeMillis(),
        elapsedRealtimeMs = SystemClock.elapsedRealtime(),
        bootCount = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1),
    )
