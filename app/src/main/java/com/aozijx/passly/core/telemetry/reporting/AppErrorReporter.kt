package com.aozijx.passly.core.telemetry.reporting

import com.aozijx.passly.core.error.model.AppError

/**
 * 错误报告器契约。
 *
 * 属于 telemetry 层，不属 error 层。error 模型不知道 telemetry 的存在。
 * 在 repository / use case 边界统一调用一次，避免重复记录。
 */
fun interface AppErrorReporter {
    fun report(
        error: AppError,
        context: ErrorReportContext
    )
}