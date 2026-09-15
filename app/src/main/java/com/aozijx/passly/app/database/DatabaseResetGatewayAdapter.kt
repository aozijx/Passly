package com.aozijx.passly.app.database

import com.aozijx.passly.core.error.mapping.fromThrowable
import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.port.DatabaseResetController
import com.aozijx.passly.feature.database.reset.DatabaseResetGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseResetGatewayAdapter @Inject constructor(
    private val controller: DatabaseResetController,
) : DatabaseResetGateway {
    override suspend fun reset(): AppResult<Unit> = withContext(Dispatchers.IO) {
        controller.reset()?.let { AppResult.Failure(AppError.fromThrowable(it)) }
            ?: AppResult.Success(Unit)
    }
}