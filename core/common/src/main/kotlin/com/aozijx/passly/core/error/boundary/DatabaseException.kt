package com.aozijx.passly.core.error.boundary

/**
 * 数据库初始化或迁移相关的低层异常。
 *
 * 对外展示前必须通过 AppErrorMapper 转成 AppError。这里的 message 使用固定安全文案，
 * 具体数据库异常细节只保留在 cause 里，避免路径、SQLCipher 信息或实现细节进入 UI。
 */
sealed class DatabaseException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {

    class MigrationFailedException(cause: Throwable) :
        DatabaseException("数据库迁移失败", cause)

    class InvalidPassphraseException(cause: Throwable? = null) :
        DatabaseException("数据库解密失败", cause)

    class InitializationException(message: String, cause: Throwable? = null) :
        DatabaseException(message, cause)
}
