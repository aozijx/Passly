package com.aozijx.passly.data.local.database.maintenance

import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 清空应用数据库中用户数据表的结果。
 */
data class DatabaseClearResult(
    val entriesDeleted: Int = 0,
    val secretsDeleted: Int = 0,
    val revisionsDeleted: Int = 0,
    val activityDeleted: Int = 0,
    val attachmentsDeleted: Int = 0,
    val attachmentResourcesDeleted: Int = 0,
    val searchTokensDeleted: Int = 0,
    /** PENDING 状态附件（未提交的暂存附件）删除行数 */
    val stagingDeleted: Int = 0
)

/**
 * 统一清空应用数据的入口。
 *
 * 业务层不应直接组合多个 [com.aozijx.passly.data.local.database.dao] 的 clear() 调用，
 * 应通过此接口统一执行。
 */
interface DatabaseCleaner {
    /** 清空所有用户数据表，返回各表删除行数。 */
    suspend fun clearAllData(): DatabaseClearResult
}

/**
 * 按子表→父表顺序执行清理，确保外键约束不受影响。
 * 同时统计草稿和暂存附件的删除数量。
 */
@Singleton
internal class DatabaseCleanerImpl @Inject constructor(
    private val databaseSession: AppDatabaseSession
) : DatabaseCleaner {

    override suspend fun clearAllData(): DatabaseClearResult = databaseSession.transaction {
        val maintenance = databaseMaintenanceDao()
        // 子表→父表顺序：先删依赖表，再删主表
        val searchTokensDeleted = maintenance.clearSearchTokens()
        val stagingDeleted = maintenance.clearPending()
        val attachmentsDeleted = maintenance.clearAttachments()
        val revisionsDeleted = maintenance.clearRevisions()
        maintenance.clearAttachmentGcQueue()
        val attachmentResourcesDeleted = maintenance.clearAttachmentResources()
        DatabaseClearResult(
            searchTokensDeleted = searchTokensDeleted,
            stagingDeleted = stagingDeleted,
            revisionsDeleted = revisionsDeleted,
            activityDeleted = maintenance.clearActivities(),
            attachmentsDeleted = attachmentsDeleted,
            attachmentResourcesDeleted = attachmentResourcesDeleted,
            secretsDeleted = maintenance.clearSecrets(),
            entriesDeleted = maintenance.clearEntries(),
        )
    }
}
