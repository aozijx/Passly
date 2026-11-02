package com.aozijx.passly.feature.backup.presentation

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.backup.contract.BackupExportFormat
import com.aozijx.passly.feature.backup.contract.BackupOperation
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupViewModelAccessTest {
    @Test
    fun `recovery mode rejects preparing export and import with operation feedback`() =
        runTest {
            val model = model(MutableAccess(AuthenticationState.RecoveryMode(1L)))
            val owner = model.onPageEntered()

            val exportEffect = async { model.effects.first() }
            model.onAction(BackupUiAction.SelectExportFormat(BackupExportFormat.JSON))
            assertEquals(BackupNoticeEffect.Failed(BackupOperation.EXPORT), exportEffect.await())
            assertFailed(model, BackupOperation.EXPORT)

            val requestId = requestImportDocument(model, owner)
            val importEffect = async { model.effects.first() }
            model.onAction(BackupUiAction.StartImport("content://backup/document", owner, requestId))
            assertEquals(BackupNoticeEffect.Failed(BackupOperation.IMPORT), importEffect.await())
            assertFailed(model, BackupOperation.IMPORT)
        }

    @Test
    fun `recovery mode rejects an already prepared export`() =
        runTest {
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
    fun `recovery mode rejects an already prepared import`() =
        runTest {
            val access = MutableAccess(AuthenticationState.Authenticated(1L))
            val model = model(access)
            val owner = model.onPageEntered()
            model.onAction(
                BackupUiAction.StartImport("content://backup/document", owner, requestImportDocument(model, owner)),
            )
            assertTrue(model.uiState.value.workflow is BackupWorkflow.ConfigureImport)

            access.authenticationState.value = AuthenticationState.RecoveryMode(2L)
            val effect = async { model.effects.first() }
            model.onAction(BackupUiAction.SubmitImport)

            assertEquals(BackupNoticeEffect.Failed(BackupOperation.IMPORT), effect.await())
            assertFailed(model, BackupOperation.IMPORT)
        }

    @Test
    fun `authenticated session permits preparing export and import`() =
        runTest {
            val model = model(MutableAccess(AuthenticationState.Authenticated(1L)))
            val owner = model.onPageEntered()

            model.onAction(BackupUiAction.SelectExportFormat(BackupExportFormat.JSON))
            assertTrue(model.uiState.value.workflow is BackupWorkflow.ConfigureExport)
            model.onAction(
                BackupUiAction.StartImport("content://backup/document", owner, requestImportDocument(model, owner)),
            )
            val import = model.uiState.value.workflow as BackupWorkflow.ConfigureImport
            assertEquals("content://backup/document", import.options.sourceUri)
        }

    @Test
    fun `late import picker result from an exited page cannot start a workflow`() =
        runTest {
            val model = model(MutableAccess(AuthenticationState.Authenticated(1L)))
            val oldPageOwner = model.onPageEntered()
            val requestId = requestImportDocument(model, oldPageOwner)

            model.onPageExited(oldPageOwner)
            val newPageOwner = model.onPageEntered()
            val newRequestId = requestImportDocument(model, newPageOwner)
            model.onAction(BackupUiAction.StartImport("content://backup/document", newPageOwner, requestId))
            assertEquals(BackupWorkflow.Idle, model.uiState.value.workflow)

            model.onAction(BackupUiAction.StartImport("content://backup/document", newPageOwner, newRequestId))
            assertTrue(model.uiState.value.workflow is BackupWorkflow.ConfigureImport)
        }

    private suspend fun requestImportDocument(
        model: BackupViewModel,
        owner: Any,
    ): String =
        coroutineScope {
            val pickerEffect = async { model.effects.first() }
            model.onAction(BackupUiAction.RequestImportDocument(owner))
            (pickerEffect.await() as BackupEffect.SelectImportDocument).requestId
        }

    private fun assertFailed(
        model: BackupViewModel,
        operation: BackupOperation,
    ) {
        val failed = model.uiState.value.workflow as BackupWorkflow.Failed
        assertEquals(operation, failed.operation)
        assertTrue(failed.error is BackupFailed)
    }

    private fun model(access: MutableAccess): BackupViewModel =
        BackupViewModel(
            backupOperationGateway = FakeBackupOperationGateway(),
            secureSessionAccessState = access,
        )

    private class MutableAccess(
        initial: AuthenticationState,
    ) : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow(initial)

        override fun isUnlocked(): Boolean = authenticationState.value !is AuthenticationState.Locked
    }
}
