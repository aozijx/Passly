package com.aozijx.passly.feature.database.reset

import com.aozijx.passly.core.error.result.AppResult

interface DatabaseResetGateway {
    suspend fun reset(): AppResult<Unit>
}
