package com.aozijx.passly.core.observability.reporting

import com.aozijx.passly.core.error.model.AppError

fun interface AppErrorReporter {
    suspend fun report(error: AppError, context: ErrorReportContext)
}
