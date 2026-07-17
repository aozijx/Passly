package com.aozijx.passly.domain.repository.security

import com.aozijx.passly.core.error.AppResult

interface RecoveryCodeRepository {
    suspend fun create(): CharArray
    suspend fun regenerate(): CharArray
    suspend fun hasRecoveryCode(): Boolean
    suspend fun verify(code: CharArray): Boolean
    suspend fun unlock(code: CharArray): AppResult<Unit>
}
