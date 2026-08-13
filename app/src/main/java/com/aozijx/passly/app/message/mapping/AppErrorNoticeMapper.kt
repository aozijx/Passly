package com.aozijx.passly.app.message.mapping

import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.model.BACKUP_FAILED
import com.aozijx.passly.core.error.model.CONFLICT
import com.aozijx.passly.core.error.model.CRYPTO_DATA_CORRUPTED
import com.aozijx.passly.core.error.model.CRYPTO_ERROR
import com.aozijx.passly.core.error.model.DATABASE_INIT_FAILED
import com.aozijx.passly.core.error.model.DATABASE_LOCKED
import com.aozijx.passly.core.error.model.NOT_FOUND
import com.aozijx.passly.core.error.model.UNEXPECTED
import com.aozijx.passly.core.telemetry.OperationCode
import com.aozijx.passly.data.message.model.NoticeCode

/**
 * 将 [AppError] 映射为 [NoticeCode]，供消息中心发布 [AppNotice]。
 *
 * Error 不直接发布 AppNotice。页面根据 error code + operation 决定映射结果。
 * 某些错误码（如 VALIDATION_ERROR、NOT_FOUND）通常不发布消息，返回 null 表示内联显示。
 */
object AppErrorNoticeMapper {

    /**
     * 将 AppError 映射为 NoticeCode。
     *
     * @param error 错误模型
     * @param operation 操作上下文（用于区分同一错误码在不同场景下的映射）
     * @return 对应的 NoticeCode，null 表示不发布消息（内联显示）
     */
    fun toNoticeCode(error: AppError, operation: OperationCode? = null): NoticeCode? {
        return when (error.code) {
            DATABASE_LOCKED,
            DATABASE_INIT_FAILED -> NoticeCode.DATABASE_OPERATION_FAILED

            BACKUP_FAILED -> when {
                operation?.value?.endsWith("export") == true -> NoticeCode.BACKUP_EXPORT_FAILED
                operation?.value?.endsWith("import") == true -> NoticeCode.BACKUP_IMPORT_FAILED
                else -> NoticeCode.BACKUP_EXPORT_FAILED
            }

            CRYPTO_ERROR,
            CRYPTO_DATA_CORRUPTED -> NoticeCode.SECURITY_ACTION_FAILED

            UNEXPECTED -> NoticeCode.DATABASE_OPERATION_FAILED

            // 以下错误码通常不发布消息，由页面内联显示
            CONFLICT,
            NOT_FOUND -> null

            else -> null
        }
    }
}