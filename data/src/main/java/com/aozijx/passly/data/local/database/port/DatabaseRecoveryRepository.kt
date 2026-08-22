package com.aozijx.passly.data.local.database.port

import com.aozijx.passly.data.local.database.model.DatabaseRecoveryPackage
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryReport
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryScan
import com.aozijx.passly.data.local.database.model.DatabaseRecoverySelection

interface DatabaseRecoveryRepository {
    suspend fun listPackages(): List<DatabaseRecoveryPackage>
    suspend fun scan(packageId: String): DatabaseRecoveryScan
    suspend fun restore(
        packageId: String,
        selection: DatabaseRecoverySelection,
    ): DatabaseRecoveryReport
    suspend fun delete(packageId: String)
}
