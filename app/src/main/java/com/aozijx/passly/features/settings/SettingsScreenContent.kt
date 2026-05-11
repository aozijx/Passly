package com.aozijx.passly.features.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.designsystem.model.VaultCardStyle
import com.aozijx.passly.features.settings.components.sections.AppearanceCustomizationSettingsSection
import com.aozijx.passly.features.settings.components.sections.BackupRestoreSettingsSection
import com.aozijx.passly.features.settings.components.sections.DataSettingsSection
import com.aozijx.passly.features.settings.components.sections.ImmersiveExperienceSettingsSection
import com.aozijx.passly.features.settings.components.sections.InteractionHabitsSettingsSection
import com.aozijx.passly.features.settings.components.sections.SecurityPrivacySettingsSection
import com.aozijx.passly.features.settings.components.sections.VaultTabsSettingsSection

internal data class SettingsContentState(
    val lockTimeout: Long,
    val isAppPasswordEnabled: Boolean,
    val isPasswordPreferredAuthFirst: Boolean,
    val isDeviceCredentialFallbackEnabled: Boolean,
    val isInvalidateKeyOnBioChange: Boolean,
    val isSecureContentEnabled: Boolean,
    val isFlipToLockEnabled: Boolean,
    val isFlipExitAndClearStackEnabled: Boolean,
    val isStatusBarAutoHide: Boolean,
    val isTopBarCollapsible: Boolean,
    val isTabBarCollapsible: Boolean,
    val isSwipeEnabled: Boolean,
    val swipeLeftAction: SwipeActionType,
    val swipeRightAction: SwipeActionType,
    val autofillUiMode: AutofillUiMode,
    val visibleVaultTabs: Set<String>?,
    val tabBarMaxTabsWithoutScroll: Int,
    val isAutoDownloadIcons: Boolean,
    val availableCardStyles: List<VaultCardStyle>,
    val passwordSelectedStyle: VaultCardStyle,
    val totpSelectedStyle: VaultCardStyle,
    val backupPathLabel: String,
    val lastExportFileLabel: String
)

internal data class SettingsContentActions(
    val onBack: () -> Unit,
    val onShowLockTimeoutDialog: () -> Unit,
    val onAppPasswordClick: () -> Unit,
    val onPasswordPreferredAuthFirstChange: (Boolean) -> Unit,
    val onDeviceCredentialFallbackToggleRequested: (Boolean) -> Unit,
    val onInvalidateKeyOnBioChangeToggle: (Boolean) -> Unit,
    val onSecureContentEnabledChange: (Boolean) -> Unit,
    val onFlipToLockEnabledChange: (Boolean) -> Unit,
    val onFlipExitAndClearStackEnabledChange: (Boolean) -> Unit,
    val onStatusBarAutoHideChange: (Boolean) -> Unit,
    val onTopBarCollapsibleChange: (Boolean) -> Unit,
    val onTabBarCollapsibleChange: (Boolean) -> Unit,
    val onSwipeEnabledChange: (Boolean) -> Unit,
    val onLeftSwipeActionClick: () -> Unit,
    val onRightSwipeActionClick: () -> Unit,
    val onToggleAutofillUiMode: () -> Unit,
    val onVisibleVaultTabsChange: (Set<String>) -> Unit,
    val onTabBarMaxTabsWithoutScrollChange: (Int) -> Unit,
    val onAutoDownloadIconsChange: (Boolean) -> Unit,
    val onPickBackupPath: () -> Unit,
    val onTestBackupWrite: () -> Unit,
    val onClearBackupPath: (() -> Unit)?,
    val onPasswordStyleSelected: (VaultCardStyle) -> Unit,
    val onTotpStyleSelected: (VaultCardStyle) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
    state: SettingsContentState,
    actions: SettingsContentActions
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                ImmersiveExperienceSettingsSection(
                    isStatusBarAutoHide = state.isStatusBarAutoHide,
                    isTopBarCollapsible = state.isTopBarCollapsible,
                    isTabBarCollapsible = state.isTabBarCollapsible,
                    onStatusBarAutoHideChange = actions.onStatusBarAutoHideChange,
                    onTopBarCollapsibleChange = actions.onTopBarCollapsibleChange,
                    onTabBarCollapsibleChange = actions.onTabBarCollapsibleChange
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                SecurityPrivacySettingsSection(
                    lockTimeout = state.lockTimeout,
                    isAppPasswordEnabled = state.isAppPasswordEnabled,
                    isPasswordPreferredAuthFirst = state.isPasswordPreferredAuthFirst,
                    isDeviceCredentialFallbackEnabled = state.isDeviceCredentialFallbackEnabled,
                    isInvalidateKeyOnBioChange = state.isInvalidateKeyOnBioChange,
                    isSecureContentEnabled = state.isSecureContentEnabled,
                    isFlipToLockEnabled = state.isFlipToLockEnabled,
                    isFlipExitAndClearStackEnabled = state.isFlipExitAndClearStackEnabled,
                    onLockTimeoutClick = actions.onShowLockTimeoutDialog,
                    onAppPasswordClick = actions.onAppPasswordClick,
                    onPasswordPreferredAuthFirstChange = actions.onPasswordPreferredAuthFirstChange,
                    onDeviceCredentialFallbackToggleRequested = actions.onDeviceCredentialFallbackToggleRequested,
                    onInvalidateKeyOnBioChangeToggle = actions.onInvalidateKeyOnBioChangeToggle,
                    onSecureContentEnabledChange = actions.onSecureContentEnabledChange,
                    onFlipToLockEnabledChange = actions.onFlipToLockEnabledChange,
                    onFlipExitAndClearStackEnabledChange = actions.onFlipExitAndClearStackEnabledChange
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                InteractionHabitsSettingsSection(
                    isSwipeEnabled = state.isSwipeEnabled,
                    swipeLeftAction = state.swipeLeftAction,
                    swipeRightAction = state.swipeRightAction,
                    autofillUiMode = state.autofillUiMode,
                    onSwipeEnabledChange = actions.onSwipeEnabledChange,
                    onLeftSwipeActionClick = actions.onLeftSwipeActionClick,
                    onRightSwipeActionClick = actions.onRightSwipeActionClick,
                    onToggleAutofillUiMode = actions.onToggleAutofillUiMode
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                VaultTabsSettingsSection(
                    visibleVaultTabs = state.visibleVaultTabs,
                    tabBarMaxTabsWithoutScroll = state.tabBarMaxTabsWithoutScroll,
                    onTabBarMaxTabsWithoutScrollChange = actions.onTabBarMaxTabsWithoutScrollChange,
                    onVisibleVaultTabsChange = actions.onVisibleVaultTabsChange
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                DataSettingsSection(
                    isAutoDownloadIcons = state.isAutoDownloadIcons,
                    onAutoDownloadIconsChange = actions.onAutoDownloadIconsChange
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                BackupRestoreSettingsSection(
                    pathLabel = state.backupPathLabel,
                    recentExportFileName = state.lastExportFileLabel,
                    onPickPath = actions.onPickBackupPath,
                    onTestWrite = actions.onTestBackupWrite,
                    onClearPath = actions.onClearBackupPath
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                AppearanceCustomizationSettingsSection(
                    availableStyles = state.availableCardStyles,
                    passwordSelectedStyle = state.passwordSelectedStyle,
                    totpSelectedStyle = state.totpSelectedStyle,
                    onPasswordStyleSelected = actions.onPasswordStyleSelected,
                    onTotpStyleSelected = actions.onTotpStyleSelected
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}