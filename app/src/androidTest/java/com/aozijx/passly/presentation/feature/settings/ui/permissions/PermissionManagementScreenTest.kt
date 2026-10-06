package com.aozijx.passly.presentation.feature.settings.ui.permissions

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
import com.aozijx.passly.presentation.feature.settings.permissions.BackupDirectoryUiAction
import com.aozijx.passly.presentation.feature.settings.permissions.BackupDirectoryUiModel
import com.aozijx.passly.presentation.feature.settings.permissions.ManifestCapabilityUiModel
import com.aozijx.passly.presentation.feature.settings.permissions.PermissionManagementScreenAction
import com.aozijx.passly.presentation.feature.settings.permissions.PermissionManagementUiState
import com.aozijx.passly.presentation.feature.settings.permissions.RuntimePermissionUiAction
import com.aozijx.passly.presentation.feature.settings.permissions.RuntimePermissionUiModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PermissionManagementScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun requestableCameraEmitsPermissionSelection() {
        val actions = mutableListOf<PermissionManagementScreenAction>()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        setContent(actions)

        composeRule.onNodeWithText(context.getString(R.string.settings_permissions_camera))
            .performClick()

        assertEquals(
            listOf(PermissionManagementScreenAction.RuntimePermissionSelected(RuntimePermission.CAMERA)),
            actions,
        )
    }

    @Test
    fun configuredDirectoryEmitsChangeAndClear() {
        val actions = mutableListOf<PermissionManagementScreenAction>()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        setContent(
            actions = actions,
            directory = BackupDirectoryUiModel(
                uri = "content://tree/backup",
                status = PersistedDirectoryAccessStatus.GRANTED,
                action = BackupDirectoryUiAction.CHANGE,
                canClear = true,
            ),
        )

        composeRule.onNodeWithText(
            context.getString(R.string.settings_permissions_backup_change),
        ).performClick()
        composeRule.onNodeWithText(
            context.getString(R.string.settings_permissions_backup_clear),
        ).performClick()

        assertEquals(
            listOf(
                PermissionManagementScreenAction.SelectBackupDirectory,
                PermissionManagementScreenAction.ClearBackupDirectory,
            ),
            actions,
        )
    }

    @Test
    fun unconfiguredDirectoryEmitsSelect() {
        val actions = mutableListOf<PermissionManagementScreenAction>()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        setContent(actions)

        composeRule.onNodeWithText(
            context.getString(R.string.settings_permissions_backup_select),
        ).performClick()

        assertEquals(
            listOf(PermissionManagementScreenAction.SelectBackupDirectory),
            actions,
        )
    }

    @Test
    fun missingDirectoryGrantEmitsReauthorize() {
        val actions = mutableListOf<PermissionManagementScreenAction>()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        setContent(
            actions = actions,
            directory = BackupDirectoryUiModel(
                uri = "content://tree/backup",
                status = PersistedDirectoryAccessStatus.GRANT_MISSING,
                action = BackupDirectoryUiAction.REAUTHORIZE,
                canClear = true,
            ),
        )

        composeRule.onNodeWithText(
            context.getString(R.string.settings_permissions_backup_reauthorize),
        ).performClick()

        assertEquals(
            listOf(PermissionManagementScreenAction.SelectBackupDirectory),
            actions,
        )
    }

    @Test
    fun capabilityRowsAreInformational() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        setContent(mutableListOf())

        composeRule.onNodeWithText(
            context.getString(R.string.settings_permissions_capability_network),
        ).assertHasNoClickAction()
    }

    private fun setContent(
        actions: MutableList<PermissionManagementScreenAction>,
        directory: BackupDirectoryUiModel = BackupDirectoryUiModel(
            uri = null,
            status = PersistedDirectoryAccessStatus.NOT_CONFIGURED,
            action = BackupDirectoryUiAction.SELECT,
            canClear = false,
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
