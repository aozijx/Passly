package com.aozijx.passly.feature.backup.internal.operation

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupOperationUseCaseTest {
    @Test
    fun `operation request is independent from presentation state`() {
        val request = BackupOperationRequest(
            operation = BackupOperation.EXPORT,
            exportFormat = BackupExportFormat.JSON,
            importMode = ImportMode.APPEND,
            includeIcons = true,
            includeAttachments = false,
            includeDeleted = true,
            includedEntryTypes = setOf(EntryType.LOGIN),
            targetUri = "content://backup",
            password = EmptySensitiveValue,
            pendingExportFileName = "backup.json",
            deleteTargetOnFailure = true,
        )

        assertEquals(BackupOperation.EXPORT, request.operation)
        assertEquals(BackupExportFormat.JSON, request.exportFormat)
        assertEquals(setOf(EntryType.LOGIN), request.includedEntryTypes)
    }
}
