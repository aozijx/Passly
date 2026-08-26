package com.aozijx.passly.app.database.recovery

import com.aozijx.passly.data.local.database.model.DatabaseRecoveryIssue
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryPackage
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryReport
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryScan
import com.aozijx.passly.data.local.database.model.DatabaseRecoverySelection
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryStatus
import com.aozijx.passly.data.local.database.port.DatabaseRecoveryRepository
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.database.recovery.RecoverableDatabaseStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DataDatabaseRecoveryGatewayTest {
    @Test
    fun `maps every package status without leaking data models`() = runTest {
        val repository = FakeRepository(
            packages = DatabaseRecoveryStatus.entries.mapIndexed { index, status ->
                DatabaseRecoveryPackage("package-$index", index.toLong(), index.toLong() + 10, status)
            },
        )

        val result = DataDatabaseRecoveryGateway(repository).packages()

        assertEquals(RecoverableDatabaseStatus.entries, result.map { it.status })
        assertEquals(listOf(0L, 1L, 2L, 3L, 4L), result.map { it.createdAtMillis })
    }

    @Test
    fun `maps scan counts and issues`() = runTest {
        val repository = FakeRepository(
            scan = DatabaseRecoveryScan(
                packageId = "package-1",
                recoverableByType = mapOf(EntryType.LOGIN to 2),
                deletedEntries = 3,
                conflictingEntries = 4,
                damagedEntries = 5,
                recoverableAttachments = 6,
                damagedResources = 7,
                issues = listOf(DatabaseRecoveryIssue("entry", "anonymous-1", "bad-tag")),
            ),
        )

        val result = DataDatabaseRecoveryGateway(repository).scan("package-1")

        assertEquals(mapOf(EntryType.LOGIN to 2), result.recoverableByType)
        assertEquals(listOf(3, 4, 5, 6, 7), listOf(result.deletedEntries, result.conflictingEntries, result.damagedEntries, result.recoverableAttachments, result.damagedResources))
        assertEquals("bad-tag", result.issues.single().reasonCode)
        assertEquals("anonymous-1", result.issues.single().anonymousRecordId)
    }

    @Test
    fun `maps selection report and forwards delete`() = runTest {
        val repository = FakeRepository(
            report = DatabaseRecoveryReport(
                packageId = "package-1",
                restoredEntries = 1,
                skippedConflicts = 2,
                restoredAttachments = 3,
                skippedResources = 4,
                restoredRevisions = 5,
                restoredLinks = 6,
                issues = listOf(DatabaseRecoveryIssue("resource", reasonCode = "missing")),
            ),
        )
        val gateway = DataDatabaseRecoveryGateway(repository)

        val result = gateway.recover("package-1", setOf(EntryType.LOGIN, EntryType.NOTE))
        gateway.delete("package-1")

        assertEquals(setOf(EntryType.LOGIN, EntryType.NOTE), repository.selection?.entryTypes)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), listOf(result.restoredEntries, result.skippedConflicts, result.restoredAttachments, result.skippedResources, result.restoredRevisions, result.restoredLinks))
        assertEquals("missing", result.issues.single().reasonCode)
        assertEquals("package-1", repository.deletedPackageId)
    }

    private class FakeRepository(
        private val packages: List<DatabaseRecoveryPackage> = emptyList(),
        private val scan: DatabaseRecoveryScan = DatabaseRecoveryScan("package", emptyMap(), 0, 0, 0, 0, 0, emptyList()),
        private val report: DatabaseRecoveryReport = DatabaseRecoveryReport("package", 0, 0, 0, 0, 0, 0, emptyList()),
    ) : DatabaseRecoveryRepository {
        var selection: DatabaseRecoverySelection? = null
        var deletedPackageId: String? = null

        override suspend fun listPackages() = packages
        override suspend fun scan(packageId: String) = scan
        override suspend fun restore(packageId: String, selection: DatabaseRecoverySelection): DatabaseRecoveryReport {
            this.selection = selection
            return report
        }
        override suspend fun delete(packageId: String) {
            deletedPackageId = packageId
        }
    }
}
