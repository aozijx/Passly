package com.aozijx.passly.feature.vault.copy

import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext

internal sealed interface SensitiveCopyResult {
    data object Copied : SensitiveCopyResult

    data object NotAuthorized : SensitiveCopyResult

    data object Unavailable : SensitiveCopyResult
}

internal suspend fun sensitiveCopyDeliveryGuard(canDeliver: () -> Boolean): () -> Boolean {
    val callerJob = currentCoroutineContext()[Job]
    return { callerJob?.isActive != false && canDeliver() }
}
