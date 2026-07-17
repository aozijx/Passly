package com.aozijx.passly.domain.usecase.auth

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.repository.security.RecoveryCodeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecoveryCodeUseCases @Inject constructor(
    private val repository: RecoveryCodeRepository
) {
    suspend fun create(): CharArray = repository.create()
    suspend fun regenerate(): CharArray = repository.regenerate()
    suspend fun hasRecoveryCode(): Boolean = repository.hasRecoveryCode()
    suspend fun verify(code: CharArray): Boolean = repository.verify(code)
    suspend fun unlock(code: CharArray): AppResult<Unit> = repository.unlock(code)
}
