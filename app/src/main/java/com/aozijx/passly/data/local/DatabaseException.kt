package com.aozijx.passly.data.local

/**
 * 数据库初始化或迁移相关的自定义异常
 */
sealed class DatabaseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    
    /**
     * 数据库迁移失败：通常由于 Schema 变更但未提供正确的 Migration 路径
     */
    class MigrationFailedException(message: String, cause: Throwable) : 
        DatabaseException("数据库迁移失败: $message", cause)

    /**
     * 数据库加密密钥错误
     */
    class InvalidPassphraseException(message: String) : 
        DatabaseException("数据库解密失败: $message")

    /**
     * 其他初始化错误
     */
    class InitializationException(message: String, cause: Throwable? = null) : 
        DatabaseException("数据库初始化异常: $message", cause)
}
