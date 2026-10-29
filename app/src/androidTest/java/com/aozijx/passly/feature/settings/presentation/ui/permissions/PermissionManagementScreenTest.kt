package com.aozijx.passly.feature.settings.presentation.ui.permissions

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.R
import com.aozijx.passly.core.permission.catalog.ManifestCapability
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.permission.model.RuntimePermissionAccess
import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import com.aozijx.passly.feature.settings.presentation.permissions.BackupDirectoryUiModel
import com.aozijx.passly.feature.settings.presentation.permissions.ManifestCapabilityUiModel
import com.aozijx.passly.feature.settings.presentation.permissions.PermissionManagementScreenAction
import com.aozijx.passly.feature.settings.presentation.permissions.PermissionManagementUiState
import com.aozijx.passly.feature.settings.presentation.permissions.RuntimePermissionUiAction
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
        setContent(
            actions = actions,
            directory = BackupDirectoryUiModel(
                label = "backup",
                status = PersistedDirectoryAccessStatus.GRANTED,
            ),
        )

        composeRule.onNodeWithText(context.getString(R.string.settings_permissions_camera))
            .performClick()

        assertEquals(
            listOf(PermissionManagementScreenAction.RuntimePermissionSelected(RuntimePermission.CAMERA)),
            actions,
        )
        composeRule.onNodeWithText(
            context.getString(R.string.settings_permissions_backup_manage),
        ).performClick()

        assertEquals(
            listOf(
                PermissionManagementScreenAction.RuntimePermissionSelected(RuntimePermission.CAMERA),
                PermissionManagementScreenAction.OpenBackupSettings,
            ),
            actions,
        )

        composeRule.onNodeWithText(
            context.getString(R.string.settings_permissions_capability_network),
        ).assertHasNoClickAction()
    }

    private fun setContent(
        actions: MutableList<PermissionManagementScreenAction>,
        directory: BackupDirectoryUiModel = BackupDirectoryUiModel(
            status = PersistedDirectoryAccessStatus.NOT_CONFIGURED,
        ),
    ) {
        composeRule.setContent {
            MaterialTheme {
                PermissionManagementScreen(
                    state = PermissionManagementUiState(
                        runtimePermissions = listOf(
                            RuntimePermissionUiModel(
                                permission = RuntimePermission.CAMERA,
                                access = RuntimePermissionAccess.REQUESTABLE,
                                action = RuntimePermissionUiAction.REQUEST,
                            ),
                        ),
                        backupDirectory = directory,
                        manifestCapabilities = listOf(
                            ManifestCapabilityUiModel(
                                capability = ManifestCapability.INTERNET,
                                interactive = false,
                            ),
                        ),
                    ),
                    onAction = actions::add,
                    onBack = {},
                )
            }
        }
    }
}
