package com.aozijx.passly.domain.usecase.database

import com.aozijx.passly.domain.repository.database.DatabaseLifecycleRepository

/**
 * 数据库生命周期用例门面。
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseLifecycleUseCases @Inject constructor(
    private val repository: DatabaseLifecycleRepository
) {
    suspend fun preWarm(): Throwable? = repository.preWarm()

    suspend fun retry(): Throwable? = repository.retry()

    fun consumeAutoRecoveryNotice(): String? = repository.consumeAutoRecoveryNotice()

    fun close() = repository.close()
}