package com.aozijx.passly.app.database.recovery

import com.aozijx.passly.data.local.database.model.DatabaseRecoveryIssue
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryPackage
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryReport
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryScan
import com.aozijx.passly.data.local.database.model.DatabaseRecoverySelection
import com.aozijx.passly.data.local.database.port.DatabaseRecoveryRepository
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.database.recovery.DatabaseRecoveryGateway
import com.aozijx.passly.feature.database.recovery.RecoverableDatabaseIssue
import com.aozijx.passly.feature.database.recovery.RecoverableDatabasePackage
import com.aozijx.passly.feature.database.recovery.RecoverableDatabaseReport
import com.aozijx.passly.feature.database.recovery.RecoverableDatabaseScan
import com.aozijx.passly.feature.database.recovery.RecoverableDatabaseStatus
import javax.inject.Inject

class DataDatabaseRecoveryGateway @Inject constructor(
    private val repository: DatabaseRecoveryRepository,
) : DatabaseRecoveryGateway {
    override suspend fun packages(): List<RecoverableDatabasePackage> =
        repository.listPackages().map(DatabaseRecoveryPackage::toFeature)

    override suspend fun scan(packageId: String): RecoverableDatabaseScan =
        repository.scan(packageId).toFeature()

    override suspend fun recover(
        packageId: String,
        selectedTypes: Set<EntryType>,
    ): RecoverableDatabaseReport = repository
        .restore(packageId, DatabaseRecoverySelection(selectedTypes))
        .toFeature()

    override suspend fun delete(packageId: String) = repository.delete(packageId)
}

private fun DatabaseRecoveryPackage.toFeature() = RecoverableDatabasePackage(
    id = id,
    createdAtMillis = createdAtEpochMs,
    sizeBytes = sizeBytes,
    status = RecoverableDatabaseStatus.valueOf(status.name),
)

private fun DatabaseRecoveryScan.toFeature() = RecoverableDatabaseScan(
    packageId, recoverableByType, deletedEntries, conflictingEntries, damagedEntries,
    recoverableAttachments, damagedResources, issues.map(DatabaseRecoveryIssue::toFeature),
)

private fun DatabaseRecoveryReport.toFeature() = RecoverableDatabaseReport(
    packageId, restoredEntries, skippedConflicts, restoredAttachments, skippedResources,
    restoredRevisions, restoredLinks, issues.map(DatabaseRecoveryIssue::toFeature),
)

private fun DatabaseRecoveryIssue.toFeature() = RecoverableDatabaseIssue(
    category, anonymousRecordId, reasonCode,
)
