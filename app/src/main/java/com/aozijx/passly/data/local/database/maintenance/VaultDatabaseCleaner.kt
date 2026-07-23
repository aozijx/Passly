package com.aozijx.passly.data.local.database.maintenance

import com.aozijx.passly.core.session.UnifiedSessionManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 清空保险库所有数据表的结果。
 */
data class ClearDatabaseResult(
    val entriesDeleted: Int = 0,
    val secretsDeleted: Int = 0,
    val revisionsDeleted: Int = 0,
    val activityDeleted: Int = 0,
    val attachmentsDeleted: Int = 0,
    val searchTokensDeleted: Int = 0,
    val keyEnvelopesDeleted: Int = 0
)

/**
 * 统一清空保险库数据的入口。
 *
 * 业务层不应直接组合多个 [com.aozijx.passly.data.local.dao] 的 clear() 调用，
 * 应通过此接口统一执行。
 */
interface VaultDatabaseCleaner {
    /** 清空所有保险库数据表，返回各表删除行数。 */
    suspend fun clearVaultData(): ClearDatabaseResult
}

@Singleton
class VaultDatabaseCleanerImpl @Inject constructor(
    private val sessionManager: UnifiedSessionManager
) : VaultDatabaseCleaner {

    override suspend fun clearVaultData(): ClearDatabaseResult = sessionManager.transaction {
        ClearDatabaseResult(
            entriesDeleted = entryDao().clear(),
            secretsDeleted = entrySecretDao().clear(),
            revisionsDeleted = entryRevisionDao().clear(),
            activityDeleted = entryActivityDao().clear(),
            attachmentsDeleted = entryAttachmentDao().clear(),
            searchTokensDeleted = searchTokenDao().clear()
        )
    }
}
