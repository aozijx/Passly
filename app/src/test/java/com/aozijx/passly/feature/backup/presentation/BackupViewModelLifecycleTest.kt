package com.aozijx.passly.feature.backup.presentation

import android.net.TestUri
import androidx.lifecycle.ViewModelStore
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
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.backup.internal.archive.BackupArchiveService
import com.aozijx.passly.feature.backup.internal.archive.platform.BackupStorageSupport
import com.aozijx.passly.feature.backup.internal.model.BackupExportRequest
import com.aozijx.passly.feature.backup.internal.model.BackupImportRequest
import com.aozijx.passly.feature.backup.internal.model.BackupImportResult
import com.aozijx.passly.feature.backup.internal.operation.BackupOperation
import com.aozijx.passly.feature.backup.internal.operation.BackupOperationInteractor
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelLifecycleTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test
    fun `submitted import owns password through route exit and suppresses old page result`() = runTest {
        val started = CompletableDeferred<BackupImportRequest>()
        val finishImport = CompletableDeferred<AppResult<BackupImportResult>>()
        val model = model { request ->
            started.complete(request)
            finishImport.await()
        }
        val oldOwner = model.onPageEntered()
        model.onAction(BackupUiAction.StartImport(TestUri, oldOwner, requestImport(model, oldOwner)))
        model.onAction(BackupUiAction.UpdatePassword("secret"))
        val password = model.uiState.value.workflow.password() as OwnedChars
        val observedEffects = mutableListOf<BackupEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            model.effects.collect(observedEffects::add)
        }

        model.onAction(BackupUiAction.SubmitImport)

        assertTrue("Main.immediate must enter the operation before dispatch returns", started.isCompleted)
        val request = started.await()
        assertArrayEquals("secret".toCharArray(), requireNotNull(request.password))
        assertFalse(password.isWiped)
        model.onPageExited(oldOwner)
        assertEquals(BackupWorkflow.Executing(BackupOperation.IMPORT), model.uiState.value.workflow)
        val currentOwner = model.onPageEntered()
        model.onAction(BackupUiAction.OpenExportOptions)
        assertEquals(BackupWorkflow.Executing(BackupOperation.IMPORT), model.uiState.value.workflow)
        finishImport.complete(AppResult.success(IMPORT_RESULT))

        assertTrue(password.isWiped)
        assertEquals(BackupWorkflow.Idle, model.uiState.value.workflow)
        assertTrue(observedEffects.isEmpty())
        model.onPageExited(currentOwner)
    }

    @Test
    fun `password is wiped and failure is shown while its page remains active`() = runTest {
        val model = model { AppResult.failure(BackupFailed()) }
        val owner = model.onPageEntered()
        model.onAction(BackupUiAction.StartImport(TestUri, owner, requestImport(model, owner)))
        model.onAction(BackupUiAction.UpdatePassword("secret"))
        val password = model.uiState.value.workflow.password() as OwnedChars

        model.onAction(BackupUiAction.SubmitImport)

        assertTrue(password.isWiped)
        assertTrue(model.uiState.value.workflow is BackupWorkflow.Failed)
        model.onPageExited(owner)
    }

    @Test
    fun `viewmodel destruction cancels import and wipes owned and operation password buffers`() = runTest {
        val started = CompletableDeferred<BackupImportRequest>()
        val model = model { request ->
            started.complete(request)
            CompletableDeferred<AppResult<BackupImportResult>>().await()
        }
        val store = ViewModelStore()
        store.put("backup", model)
        val owner = model.onPageEntered()
        model.onAction(BackupUiAction.StartImport(TestUri, owner, requestImport(model, owner)))
        model.onAction(BackupUiAction.UpdatePassword("secret"))
        val password = model.uiState.value.workflow.password() as OwnedChars
        model.onAction(BackupUiAction.SubmitImport)
        val request = started.await()

        store.clear()

        assertTrue(password.isWiped)
        assertTrue(requireNotNull(request.password).all { it == '\u0000' })
    }

    private suspend fun requestImport(model: BackupViewModel, owner: Any): String = coroutineScope {
        val effect = async { model.effects.first() }
        model.onAction(BackupUiAction.RequestImportDocument(owner))
        (effect.await() as BackupEffect.SelectImportDocument).requestId
    }

    private fun model(performImport: suspend (BackupImportRequest) -> AppResult<BackupImportResult>) =
        BackupViewModel(
            backupOperationInteractor = BackupOperationInteractor(
                settingsSource = object : BackupDirectorySettingsSource {
                    override val backupDirectoryUri = flowOf(null)
                },
                backupService = object : BackupArchiveService {
                    override suspend fun export(request: BackupExportRequest): AppResult<Unit> =
                        error("Export is not used in these tests")

                    override suspend fun import(request: BackupImportRequest) = performImport(request)
                },
                storageSupport = object : BackupStorageSupport {
                    override fun buildBackupFileName(extension: String, timestamp: Long) = "backup.$extension"
                    override suspend fun createNamedExportTarget(
                        directoryTreeUri: String,
                        fileName: String,
                        mimeType: String,
                    ) = Result.failure<String>(error("Unused"))

                    override suspend fun deleteDocument(uri: String) = false
                    override suspend fun checkDirectoryWritable(directoryTreeUri: String) = Result.success(Unit)
                },
                authorizationGate = object : AuthorizationGate {
                    override suspend fun <T> authorize(
                        scope: AuthorizationScope,
                        input: AuthInput,
                        block: suspend (AuthorizationPermit) -> T,
                    ): AuthorizationResult<T> = AuthorizationResult.Allowed(
                        block(object : AuthorizationPermit {}),
                    )
                },
                errorReporter = AppErrorReporter { _, _ -> Unit },
            ),
            secureSessionAccessState = object : SecureSessionAccessState {
                override val authenticationState = MutableStateFlow<AuthenticationState>(
                    AuthenticationState.Authenticated(1L),
                )

                override fun isUnlocked() = true
            },
        )

    private companion object {
        val IMPORT_RESULT = BackupImportResult(
            importedEntryCount = 1,
            existingEntryCount = 0,
            skippedEntryCount = 0,
            ignoredFieldCount = 0,
            prunedLinkCount = 0,
            prunedResourceCount = 0,
        )
    }
}
