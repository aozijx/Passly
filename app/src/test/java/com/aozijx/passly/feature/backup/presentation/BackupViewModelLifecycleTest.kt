package com.aozijx.passly.feature.backup.presentation

import androidx.lifecycle.ViewModelStore
import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.observability.reporting.AppErrorReporter
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.backup.contract.BackupExecutionResult
import com.aozijx.passly.feature.backup.contract.BackupExportFormat
import com.aozijx.passly.feature.backup.contract.BackupImportOperationRequest
import com.aozijx.passly.feature.backup.contract.BackupImportResult
import com.aozijx.passly.feature.backup.contract.BackupOperation
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelLifecycleTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test
    fun `submitted import owns password through route exit and suppresses old page result`() =
        runTest {
            val started = CompletableDeferred<BackupImportOperationRequest>()
            val finishImport = CompletableDeferred<BackupExecutionResult>()
            val model =
                model { request ->
                    started.complete(request)
                    finishImport.await()
                }
            val oldOwner = model.onPageEntered()
            model.onAction(BackupUiAction.StartImport("content://backup/document", oldOwner, requestImport(model, oldOwner)))
            model.onAction(BackupUiAction.UpdatePassword("secret"))
            val password =
                model.uiState.value.workflow
                    .password() as OwnedChars
            val observedEffects = mutableListOf<BackupEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                model.effects.collect(observedEffects::add)
            }

            model.onAction(BackupUiAction.SubmitImport)

            assertTrue("Main.immediate must enter the operation before dispatch returns", started.isCompleted)
            val request = started.await()
            val operationPassword = request.password as OwnedChars
            assertEquals(6, operationPassword.length)
            assertFalse(operationPassword.isWiped)
            assertFalse(password.isWiped)
            model.onPageExited(oldOwner)
            assertEquals(BackupWorkflow.Executing(BackupOperation.IMPORT), model.uiState.value.workflow)
            val currentOwner = model.onPageEntered()
            model.onAction(BackupUiAction.OpenExportOptions)
            assertEquals(BackupWorkflow.Executing(BackupOperation.IMPORT), model.uiState.value.workflow)
            finishImport.complete(BackupExecutionResult.ImportSucceeded(IMPORT_RESULT))

            assertTrue(password.isWiped)
            assertEquals(BackupWorkflow.Idle, model.uiState.value.workflow)
            assertTrue(observedEffects.isEmpty())
            model.onPageExited(currentOwner)
        }

    @Test
    fun `password is wiped and failure is shown while its page remains active`() =
        runTest {
            val model = model { BackupExecutionResult.Failure(BackupFailed()) }
            val owner = model.onPageEntered()
            model.onAction(BackupUiAction.StartImport("content://backup/document", owner, requestImport(model, owner)))
            model.onAction(BackupUiAction.UpdatePassword("secret"))
            val password =
                model.uiState.value.workflow
                    .password() as OwnedChars

            model.onAction(BackupUiAction.SubmitImport)

            assertTrue(password.isWiped)
            assertTrue(model.uiState.value.workflow is BackupWorkflow.Failed)
            model.onPageExited(owner)
        }

    @Test
    fun `thrown ordinary operation failure ends workflow logs and wipes password`() =
        runTest {
            val failure = IOException("gateway failure")
            val reports = mutableListOf<AppError>()
            val model =
                model(errorReporter = AppErrorReporter { error, _ -> reports += error }) {
                    throw failure
                }
            val owner = model.onPageEntered()
            val requestId = requestImport(model, owner)
            model.onAction(BackupUiAction.StartImport("content://backup/document", owner, requestId))
            model.onAction(BackupUiAction.UpdatePassword("secret"))
            val password = model.uiState.value.workflow.password() as OwnedChars
            val effect = async { model.effects.first() }

            model.onAction(BackupUiAction.SubmitImport)
            runCurrent()

            assertEquals(BackupNoticeEffect.Failed(BackupOperation.IMPORT), effect.await())
            assertTrue(model.uiState.value.workflow is BackupWorkflow.Failed)
            assertTrue(password.isWiped)
            assertEquals(1, reports.size)
            model.onPageExited(owner)
        }

    @Test
    fun `viewmodel destruction cancels import and wipes the owned password value`() =
        runTest {
            val started = CompletableDeferred<BackupImportOperationRequest>()
            val model =
                model { request ->
                    started.complete(request)
                    CompletableDeferred<BackupExecutionResult>().await()
                }
            val store = ViewModelStore()
            store.put("backup", model)
            val owner = model.onPageEntered()
            model.onAction(BackupUiAction.StartImport("content://backup/document", owner, requestImport(model, owner)))
            model.onAction(BackupUiAction.UpdatePassword("secret"))
            val password =
                model.uiState.value.workflow
                    .password() as OwnedChars
            model.onAction(BackupUiAction.SubmitImport)
            val request = started.await()

            store.clear()

            assertTrue(password.isWiped)
            assertTrue((request.password as OwnedChars).isWiped)
        }

    @Test
    fun `full notice queue drops notices and keeps export target selection retryable`() =
        runTest {
            val model = model { BackupExecutionResult.ExportSucceeded }
            val owner = model.onPageEntered()

            repeat(100) {
                model.onAction(BackupUiAction.CheckDirectoryPermission(null))
                runCurrent()
            }
            assertEquals(BackupWorkflow.Idle, model.uiState.value.workflow)

            model.onAction(BackupUiAction.OpenExportOptions)
            model.onAction(BackupUiAction.SelectExportFormat(BackupExportFormat.JSON))
            assertTrue(model.uiState.value.workflow is BackupWorkflow.ConfigureExport)
            model.onAction(BackupUiAction.SubmitExport(useConfiguredDirectory = false))

            assertTrue(
                "A dropped document-picker command must leave the export form retryable",
                model.uiState.value.workflow is BackupWorkflow.ConfigureExport,
            )
            model.onPageExited(owner)
        }

    @Test
    fun `late export target result from the previous page cannot start export`() =
        runTest {
            val gateway = FakeBackupOperationGateway()
            val model =
                BackupViewModel(
                    backupOperationGateway = gateway,
                    errorReporter = AppErrorReporter { _, _ -> Unit },
                    secureSessionAccessState =
                        object : SecureSessionAccessState {
                            override val authenticationState =
                                MutableStateFlow<AuthenticationState>(
                                    AuthenticationState.Authenticated(1L),
                                )

                            override fun isUnlocked() = true
                        },
                )
            val oldOwner = model.onPageEntered()
            model.onAction(BackupUiAction.OpenExportOptions)
            model.onAction(BackupUiAction.SelectExportFormat(BackupExportFormat.JSON))
            val pickerEffect = async { model.effects.first() }
            model.onAction(BackupUiAction.SubmitExport(useConfiguredDirectory = false))
            val picker = pickerEffect.await() as BackupEffect.SelectExportDocument

            model.onPageEntered()
            model.onPageExited(oldOwner)
            model.onAction(
                BackupUiAction.StartExport(
                    uri = "content://stale",
                    pageOwner = oldOwner,
                    requestId = picker.requestId,
                    deleteOnFailure = true,
                ),
            )
            runCurrent()

            assertEquals(BackupWorkflow.Idle, model.uiState.value.workflow)
            assertEquals(null, gateway.exportedUri)
        }

    private suspend fun requestImport(
        model: BackupViewModel,
        owner: Any,
    ): String =
        coroutineScope {
            val effect = async { model.effects.first() }
            model.onAction(BackupUiAction.RequestImportDocument(owner))
            (effect.await() as BackupEffect.SelectImportDocument).requestId
        }

    private fun model(
        errorReporter: AppErrorReporter = AppErrorReporter { _, _ -> Unit },
        performImport: suspend (BackupImportOperationRequest) -> BackupExecutionResult,
    ) =
        BackupViewModel(
            backupOperationGateway = FakeBackupOperationGateway(performImport),
            errorReporter = errorReporter,
            secureSessionAccessState =
                object : SecureSessionAccessState {
                    override val authenticationState =
                        MutableStateFlow<AuthenticationState>(
                            AuthenticationState.Authenticated(1L),
                        )

                    override fun isUnlocked() = true
                },
        )

    private companion object {
        val IMPORT_RESULT =
            BackupImportResult(
                importedEntryCount = 1,
                existingEntryCount = 0,
                skippedEntryCount = 0,
                ignoredFieldCount = 0,
                prunedLinkCount = 0,
                prunedResourceCount = 0,
            )
    }
}
