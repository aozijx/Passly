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
    /** 草稿表删除行数 */
    val draftsDeleted: Int = 0,
    /** PENDING 状态附件（未提交的暂存附件）删除行数 */
    val stagingDeleted: Int = 0
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

/**
 * 按子表→父表顺序执行清理，确保外键约束不受影响。
 * 同时统计草稿和暂存附件的删除数量。
 */
@Singleton
class VaultDatabaseCleanerImpl @Inject constructor(
    private val sessionManager: UnifiedSessionManager
) : VaultDatabaseCleaner {

    override suspend fun clearVaultData(): ClearDatabaseResult = sessionManager.transaction {
        val maintenance = vaultMaintenanceDao()
        // 子表→父表顺序：先删依赖表，再删主表
        ClearDatabaseResult(
            searchTokensDeleted = maintenance.clearSearchTokens(),
            draftsDeleted = maintenance.clearDrafts(),
            stagingDeleted = maintenance.clearPending(),
            revisionsDeleted = maintenance.clearRevisions(),
            activityDeleted = maintenance.clearActivities(),
            attachmentsDeleted = maintenance.clearAttachments(),
            secretsDeleted = maintenance.clearSecrets(),
            entriesDeleted = maintenance.clearEntries()
        )
    }
}
