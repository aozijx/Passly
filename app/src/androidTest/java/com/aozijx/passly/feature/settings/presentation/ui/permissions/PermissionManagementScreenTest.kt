package com.aozijx.passly.feature.settings.presentation.ui.permissions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.R
import com.aozijx.passly.app.settings.ReadFailure
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.core.permission.catalog.ManifestCapability
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.permission.model.RuntimePermissionAccess
import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import com.aozijx.passly.feature.settings.presentation.permissions.BackupDirectoryUiModel
import com.aozijx.passly.feature.settings.presentation.permissions.ManifestCapabilityUiModel
import com.aozijx.passly.feature.settings.presentation.permissions.PermissionManagementScreenAction
import com.aozijx.passly.feature.settings.presentation.permissions.PermissionManagementUiState
import com.aozijx.passly.feature.settings.presentation.permissions.RuntimePermissionUiModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PermissionManagementScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun screenEmitsActionsAndKeepsCapabilityRowsInformational() {
        val actions = mutableListOf<PermissionManagementScreenAction>()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uiState =
            mutableStateOf(
                buildState(
                    backupDirectory =
                        BackupDirectoryUiModel(
                            status = PersistedDirectoryAccessStatus.GRANTED,
                        ),
                ),
            )
        composeRule.setContent {
            MaterialTheme {
                PermissionManagementScreen(
                    state = uiState.value,
                    onAction = actions::add,
                    onBack = {},
                )
            }
        }

        composeRule
            .onNodeWithText(context.getString(R.string.settings_permissions_camera))
            .performClick()

        assertEquals(
            listOf(PermissionManagementScreenAction.RuntimePermissionSelected(RuntimePermission.CAMERA)),
            actions,
        )
        composeRule
            .onNodeWithText(
                context.getString(R.string.settings_permissions_backup_manage),
            ).performClick()

        assertEquals(
            listOf(
                PermissionManagementScreenAction.RuntimePermissionSelected(RuntimePermission.CAMERA),
                PermissionManagementScreenAction.OpenBackupSettings,
            ),
            actions,
        )

        composeRule
            .onNodeWithText(
                context.getString(R.string.settings_permissions_capability_network),
            ).assertHasNoClickAction()

        actions.clear()
        composeRule.runOnIdle {
            uiState.value =
                buildState(
                    backupDirectoryReadState = RecoverableReadState.Failed(ReadFailure.IO),
                )
        }
        composeRule.onNodeWithText(context.getString(R.string.retry)).performClick()
        assertEquals(listOf(PermissionManagementScreenAction.RetryBackupDirectoryRead), actions)
    }

    private fun buildState(
        backupDirectory: BackupDirectoryUiModel =
            BackupDirectoryUiModel(
                status = PersistedDirectoryAccessStatus.NOT_CONFIGURED,
            ),
        backupDirectoryReadState: RecoverableReadState<String?> =
            RecoverableReadState.Ready("content://backup"),
    ) = PermissionManagementUiState(
        runtimePermissions =
            listOf(
                RuntimePermissionUiModel(
                    permission = RuntimePermission.CAMERA,
                    access = RuntimePermissionAccess.REQUESTABLE,
                ),
            ),
        backupDirectory = backupDirectory,
        backupDirectoryReadState = backupDirectoryReadState,
        manifestCapabilities =
            listOf(
                ManifestCapabilityUiModel(
                    capability = ManifestCapability.INTERNET,
                    interactive = false,
                ),
            ),
    )
}
