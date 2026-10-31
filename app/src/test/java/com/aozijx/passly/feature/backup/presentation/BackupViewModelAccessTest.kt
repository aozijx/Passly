package com.aozijx.passly.feature.backup.presentation

import android.net.Uri
import android.net.TestUri
import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.observability.reporting.AppErrorReporter
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsSource
import com.aozijx.passly.feature.backup.internal.archive.BackupArchiveService
import com.aozijx.passly.feature.backup.internal.archive.platform.BackupStorageSupport
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.model.BackupExportRequest
import com.aozijx.passly.feature.backup.internal.model.BackupImportRequest
import com.aozijx.passly.feature.backup.internal.model.BackupImportResult
import com.aozijx.passly.feature.backup.internal.operation.BackupOperation
import com.aozijx.passly.feature.backup.internal.operation.BackupOperationInteractor
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupViewModelAccessTest {
    @Test
    fun `recovery mode rejects preparing export and import with operation feedback`() = runTest {
        val model = model(MutableAccess(AuthenticationState.RecoveryMode(1L)))
        model.onPageEntered()

        val exportEffect = async { model.effects.first() }
        model.onAction(BackupUiAction.SelectExportFormat(BackupExportFormat.JSON))
        assertEquals(BackupNoticeEffect.Failed(BackupOperation.EXPORT), exportEffect.await())
        assertFailed(model, BackupOperation.EXPORT)

        val importEffect = async { model.effects.first() }
        model.onAction(BackupUiAction.StartImport(TestUri))
        assertEquals(BackupNoticeEffect.Failed(BackupOperation.IMPORT), importEffect.await())
        assertFailed(model, BackupOperation.IMPORT)
    }

    @Test
    fun `recovery mode rejects an already prepared export`() = runTest {
        val access = MutableAccess(AuthenticationState.Authenticated(1L))
        val model = model(access)
        model.onPageEntered()
        model.onAction(BackupUiAction.SelectExportFormat(BackupExportFormat.JSON))
        assertTrue(model.uiState.value.workflow is BackupWorkflow.ConfigureExport)

        access.authenticationState.value = AuthenticationState.RecoveryMode(2L)
        val effect = async { model.effects.first() }
        model.onAction(BackupUiAction.SubmitExport(useConfiguredDirectory = false))

        assertEquals(BackupNoticeEffect.Failed(BackupOperation.EXPORT), effect.await())
        assertFailed(model, BackupOperation.EXPORT)
    }

    @Test
    fun `recovery mode rejects an already prepared import`() = runTest {
        val access = MutableAccess(AuthenticationState.Authenticated(1L))
        val model = model(access)
        model.onPageEntered()
        model.onAction(BackupUiAction.StartImport(TestUri))
        assertTrue(model.uiState.value.workflow is BackupWorkflow.ConfigureImport)

        access.authenticationState.value = AuthenticationState.RecoveryMode(2L)
        val effect = async { model.effects.first() }
        model.onAction(BackupUiAction.SubmitImport)

        assertEquals(BackupNoticeEffect.Failed(BackupOperation.IMPORT), effect.await())
        assertFailed(model, BackupOperation.IMPORT)
    }

    @Test
    fun `authenticated session permits preparing export and import`() = runTest {
        val model = model(MutableAccess(AuthenticationState.Authenticated(1L)))

        model.onAction(BackupUiAction.SelectExportFormat(BackupExportFormat.JSON))
        assertTrue(model.uiState.value.workflow is BackupWorkflow.ConfigureExport)
        model.onAction(BackupUiAction.StartImport(TestUri))
        assertTrue(model.uiState.value.workflow is BackupWorkflow.ConfigureImport)
    }

    private fun assertFailed(model: BackupViewModel, operation: BackupOperation) {
        val failed = model.uiState.value.workflow as BackupWorkflow.Failed
        assertEquals(operation, failed.operation)
        assertTrue(failed.error is BackupFailed)
    }

    private fun model(access: MutableAccess): BackupViewModel = BackupViewModel(
        backupOperationInteractor = interactor(),
        secureSessionAccessState = access,
    )

    private fun interactor() = BackupOperationInteractor(
        settingsSource = object : BackupDirectorySettingsSource {
            override val backupDirectoryUri = flowOf(null)
        },
        backupService = object : BackupArchiveService {
            override suspend fun export(request: BackupExportRequest): AppResult<Unit> =
                error("Backup operation must not execute in access-state tests")

            override suspend fun import(request: BackupImportRequest): AppResult<BackupImportResult> =
                error("Backup operation must not execute in access-state tests")
        },
        storageSupport = object : BackupStorageSupport {
            override fun buildBackupFileName(extension: String, timestamp: Long) = "backup.$extension"

            override suspend fun createNamedExportTarget(
                directoryTreeUri: String,
                fileName: String,
                mimeType: String,
            ) = Result.failure<String>(error("Unused in access-state tests"))

            override suspend fun deleteDocument(uri: String) = false

            override suspend fun checkDirectoryWritable(directoryTreeUri: String) = Result.success(Unit)
        },
        authorizationGate = object : AuthorizationGate {
            override suspend fun <T> authorize(
                scope: AuthorizationScope,
                input: AuthInput,
                block: suspend (AuthorizationPermit) -> T,
            ): AuthorizationResult<T> = error("Backup operation must not execute in access-state tests")
        },
        errorReporter = AppErrorReporter { _, _ -> Unit },
    )

    private class MutableAccess(initial: AuthenticationState) : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow(initial)

        override fun isUnlocked(): Boolean = authenticationState.value !is AuthenticationState.Locked
    }

}
