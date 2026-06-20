package com.aozijx.passly.domain.repository.backup

import android.net.Uri
import com.aozijx.passly.domain.model.BackupImportMode
import java.io.File

/**
 * 备份/恢复仓库接口。
 */
interface BackupRepository {
    
    /**
     * 导出加密备份文件。
     */
    suspend fun exportEncryptedBackup(
        uri: Uri,
        password: CharArray,
        includeImages: Boolean
    )

    /**
     * 导出明文 JSON 备份。
     */
    suspend fun exportPlainBackup(uri: Uri)

    /**
     * 导出紧急备份文件。
     */
    suspend fun exportEmergencyBackup(): File

    /**
     * 从备份文件中导入。
     */
    suspend fun importBackup(
        uri: Uri,
        password: CharArray,
        mode: BackupImportMode
    )

    /**
     * 测试目录写入权限。
     */
    suspend fun testDirectoryWritePermission(directoryUri: String)
}