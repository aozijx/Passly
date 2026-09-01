package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.result.AppResult

internal object ReplacedIconCleanupPolicy {
    fun candidate(oldPath: String?, newPath: String?): String? =
        oldPath?.takeIf { it.isNotBlank() && it != newPath }

    fun canDelete(referenceCount: Int?): Boolean = referenceCount == 0
}

internal suspend fun AppResult<Unit>.cleanReplacedIconOnSuccess(
    oldPath: String?,
    newPath: String?,
    cleanup: suspend (oldPath: String?, newPath: String?) -> Unit,
): AppResult<Unit> = onSuccessSuspend {
    cleanup(oldPath, newPath)
}
