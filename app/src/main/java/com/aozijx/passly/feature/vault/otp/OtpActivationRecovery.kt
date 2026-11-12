package com.aozijx.passly.feature.vault.otp

/** Mutually exclusive recovery phases for one requested OTP entry. */
internal sealed interface OtpActivationRecovery {
    data object Active : OtpActivationRecovery

    data class RetryScheduled(
        val attempt: Int,
        val atMillis: Long,
    ) : OtpActivationRecovery

    data class Retrying(
        val attempt: Int,
    ) : OtpActivationRecovery

    data object AwaitingFreshAuthentication : OtpActivationRecovery

    data object PermanentlyFailed : OtpActivationRecovery
}

internal sealed interface OtpActivationRecoveryEvent {
    data class RetryableFailure(
        val nowMillis: Long,
    ) : OtpActivationRecoveryEvent

    data object FreshAuthenticationRequired : OtpActivationRecoveryEvent

    data object PermanentFailure : OtpActivationRecoveryEvent

    data object Recovered : OtpActivationRecoveryEvent

    data object RetryElapsed : OtpActivationRecoveryEvent

    data object FreshAuthenticationCompleted : OtpActivationRecoveryEvent
}

/** Pure recovery transition policy; Runtime performs scheduling and side effects. */
internal object OtpActivationRecoveryReducer {
    fun reduce(
        state: OtpActivationRecovery,
        event: OtpActivationRecoveryEvent,
    ): OtpActivationRecovery =
        when (event) {
            is OtpActivationRecoveryEvent.RetryableFailure -> {
                val attempt =
                    (
                        when (state) {
                            is OtpActivationRecovery.RetryScheduled -> state.attempt

                            is OtpActivationRecovery.Retrying -> state.attempt

                            OtpActivationRecovery.AwaitingFreshAuthentication,
                            OtpActivationRecovery.PermanentlyFailed,
                            OtpActivationRecovery.Active,
                            -> 0
                        }
                    ).plus(1)
                        .coerceAtMost(MAX_RETRY_ATTEMPTS)
                OtpActivationRecovery.RetryScheduled(
                    attempt = attempt,
                    atMillis = event.nowMillis + retryDelayMillis(attempt - 1),
                )
            }

            OtpActivationRecoveryEvent.FreshAuthenticationRequired -> {
                OtpActivationRecovery.AwaitingFreshAuthentication
            }

            OtpActivationRecoveryEvent.PermanentFailure -> {
                OtpActivationRecovery.PermanentlyFailed
            }

            OtpActivationRecoveryEvent.RetryElapsed -> {
                (state as? OtpActivationRecovery.RetryScheduled)?.let { OtpActivationRecovery.Retrying(it.attempt) }
                    ?: OtpActivationRecovery.Active
            }

            OtpActivationRecoveryEvent.Recovered,
            OtpActivationRecoveryEvent.FreshAuthenticationCompleted,
            -> {
                OtpActivationRecovery.Active
            }
        }

    private fun retryDelayMillis(attempt: Int): Long {
        val backoffIndex = attempt.coerceAtMost(MAX_RETRY_BACKOFF_SHIFT)
        return (INITIAL_RETRY_DELAY_MILLIS * (1L shl backoffIndex)).coerceAtMost(MAX_RETRY_DELAY_MILLIS)
    }

    private const val INITIAL_RETRY_DELAY_MILLIS = 1_000L
    private const val MAX_RETRY_DELAY_MILLIS = 30_000L
    private const val MAX_RETRY_ATTEMPTS = 6
    private const val MAX_RETRY_BACKOFF_SHIFT = 5
}
