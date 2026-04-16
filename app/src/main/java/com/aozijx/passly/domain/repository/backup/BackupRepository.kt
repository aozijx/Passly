package com.aozijx.passly.domain.repository.backup

import android.net.Uri
import com.aozijx.passly.core.backup.BackupImportMode
import java.io.File

/**
 * 备份/恢复仓库接口。
 * 内聚了所有与备份文件处理、加密衍生、以及数据库导入导出的核心逻辑。
 */
interface BackupRepository {
    
    /**
     * 导出加密备份文件（Argon2id + AES-GCM + ZIP）。
     */
    suspend fun exportEncryptedBackup(
        uri: Uri,
        password: CharArray,
        includeImages: Boolean
    ): Result<Unit>

    /**
     * 导出明文 JSON 备份（解密敏感字段，不含图片）。
     */
    suspend fun exportPlainBackup(uri: Uri): Result<Unit>

    /**
     * 导出紧急备份文件（用于数据库错误时的抢救，保存到私有目录）。
     */
    suspend fun exportEmergencyBackup(): Result<File>

    /**
     * 从加密备份文件或紧急明文备份中导入。
     */
    suspend fun importBackup(
        uri: Uri,
        password: CharArray,
        mode: BackupImportMode
    ): Result<Unit>

    /**
     * 测试备份目录的写入权限。
     */
    suspend fun testDirectoryWritePermission(directoryUri: String): Result<Unit>
}