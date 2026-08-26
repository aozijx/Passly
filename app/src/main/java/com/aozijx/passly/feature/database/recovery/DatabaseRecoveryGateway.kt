package com.aozijx.passly.feature.database.recovery

import com.aozijx.passly.domain.entry.model.EntryType

interface DatabaseRecoveryGateway {
    suspend fun packages(): List<RecoverableDatabasePackage>
    suspend fun scan(packageId: String): RecoverableDatabaseScan
    suspend fun recover(
        packageId: String,
        selectedTypes: Set<EntryType>,
    ): RecoverableDatabaseReport
    suspend fun delete(packageId: String)
}
