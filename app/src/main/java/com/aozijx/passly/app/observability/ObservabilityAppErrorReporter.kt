package com.aozijx.passly.app.observability

import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.reporting.AppErrorReporter
import com.aozijx.passly.core.observability.reporting.ErrorReportContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObservabilityAppErrorReporter @Inject constructor(
    private val logger: Logger,
) : AppErrorReporter {
    private val reportedErrorIds = LinkedHashSet<String>()

    override suspend fun report(error: AppError, context: ErrorReportContext) {
        if (!claim(error.errorId)) return
        logger.inSpan(context.operation) {
            logger.log(LogStatements.ApplicationError.failure(error, context))
        }
    }

    private fun claim(errorId: String): Boolean = synchronized(reportedErrorIds) {
        if (!reportedErrorIds.add(errorId)) return@synchronized false
        while (reportedErrorIds.size > MAX_REPORTED_IDS) {
            reportedErrorIds.iterator().run {
                next()
                remove()
            }
        }
        true
    }

    private companion object {
        const val MAX_REPORTED_IDS = 256
    }
}
