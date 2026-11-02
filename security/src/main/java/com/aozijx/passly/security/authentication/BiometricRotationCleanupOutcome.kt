package com.aozijx.passly.security.authentication

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal enum class BiometricRotationCleanupOutcome { COMPLETE, PENDING }

/** A committed binding cannot be rolled back because its post-commit cleanup needs another attempt. */
internal suspend fun finishCommittedBiometricRotation(cleanup: suspend () -> Boolean): BiometricRotationCleanupOutcome =
    withContext(NonCancellable) {
        try {
            if (cleanup()) BiometricRotationCleanupOutcome.COMPLETE else BiometricRotationCleanupOutcome.PENDING
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            BiometricRotationCleanupOutcome.PENDING
        }
    }
