package com.aozijx.passly.data.database.port

import com.aozijx.passly.data.database.model.DatabaseRecoveryPackage
import com.aozijx.passly.data.database.model.DatabaseRecoveryReport
import com.aozijx.passly.data.database.model.DatabaseRecoveryScan
import com.aozijx.passly.data.database.model.DatabaseRecoverySelection

interface DatabaseRecoveryRepository {
    suspend fun listPackages(): List<DatabaseRecoveryPackage>
    suspend fun scan(packageId: String): DatabaseRecoveryScan
    suspend fun restore(
        packageId: String,
        selection: DatabaseRecoverySelection,
    ): DatabaseRecoveryReport
    suspend fun delete(packageId: String)
}
