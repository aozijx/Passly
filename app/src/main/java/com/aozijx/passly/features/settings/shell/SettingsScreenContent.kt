package com.aozijx.passly.features.settings.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.domain.model.VaultCardStyle
import com.aozijx.passly.features.settings.appearance.DisplayAppearanceDetail
import com.aozijx.passly.features.settings.data.DataManagementDetail
import com.aozijx.passly.features.settings.interaction.InteractionDetail
import com.aozijx.passly.features.settings.internal.SettingsGroup
import com.aozijx.passly.features.settings.security.SecurityPrivacyDetail

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
    actions: SettingsContentActions,
    onUpdateInteraction: () -> Unit = {}
) {
    var selectedGroup by remember { mutableStateOf<SettingsGroup?>(null) }

    BackHandler(enabled = selectedGroup != null) {
        selectedGroup = null
    }

    AnimatedContent(
        targetState = selectedGroup,
        transitionSpec = {
            if (targetState == null) {
                slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
            } else {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            }.using(SizeTransform(clip = false))
        },
        label = "SettingsNavigation"
    ) { group ->
        when (group) {
            null -> SettingsMainPage(
                state = state,
                actions = actions,
                onUpdateInteraction = onUpdateInteraction,
                onGroupClick = { selectedGroup = it }
            )

            SettingsGroup.SECURITY_PRIVACY -> SettingsSecondaryPage(
                title = group.title,
                onBack = { selectedGroup = null }
            ) {
                item { SecurityPrivacyDetail(state, actions) }
            }

            SettingsGroup.DISPLAY_APPEARANCE -> SettingsSecondaryPage(
                title = group.title,
                onBack = { selectedGroup = null }
            ) {
                item { DisplayAppearanceDetail(state, actions) }
            }

            SettingsGroup.INTERACTION -> SettingsSecondaryPage(
                title = group.title,
                onBack = { selectedGroup = null }
            ) {
                item { InteractionDetail(state, actions) }
            }

            SettingsGroup.DATA_MANAGEMENT -> SettingsSecondaryPage(
                title = group.title,
                onBack = { selectedGroup = null }
            ) {
                item { DataManagementDetail(state, actions) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMainPage(
    state: SettingsContentState,
    actions: SettingsContentActions,
    onUpdateInteraction: () -> Unit,
    onGroupClick: (SettingsGroup) -> Unit
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onUpdateInteraction
            ),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "设置",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
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

            SettingsGroup.entries.forEach { group ->
                item {
                    SettingsGroupCard(
                        group = group,
                        onClick = { onGroupClick(group) }
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun SettingsGroupCard(
    group: SettingsGroup,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        Modifier.drawBehind {
                            drawRect(group.accentColor.copy(alpha = 0.12f))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = group.icon,
                    contentDescription = null,
                    tint = group.accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = group.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}