package com.aozijx.passly.ui.features.settings.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.ui.features.settings.appearance.AppearanceDetail
import com.aozijx.passly.ui.features.settings.appearance.InterfaceDetail
import com.aozijx.passly.ui.features.settings.components.navigationSettingsItem
import com.aozijx.passly.ui.features.settings.data.DataManagementDetail
import com.aozijx.passly.ui.features.settings.general.GeneralDetail
import com.aozijx.passly.ui.features.settings.interaction.InteractionDetail
import com.aozijx.passly.ui.features.settings.internal.SettingsContentActions
import com.aozijx.passly.ui.features.settings.internal.SettingsContentState
import com.aozijx.passly.ui.features.settings.internal.SettingsGroup
import com.aozijx.passly.ui.features.settings.security.PrivacyDetail
import com.aozijx.passly.ui.features.settings.security.SecurityDetail

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
                actions = actions,
                onUpdateInteraction = onUpdateInteraction,
                onGroupClick = { selectedGroup = it }
            )

            SettingsGroup.SECURITY -> SettingsSecondaryPage(
                title = group.title,
                onBack = { selectedGroup = null }
            ) {
                item { SecurityDetail(state, actions) }
            }

            SettingsGroup.PRIVACY -> SettingsSecondaryPage(
                title = group.title,
                onBack = { selectedGroup = null }
            ) {
                item { PrivacyDetail(state, actions) }
            }

            SettingsGroup.APPEARANCE -> SettingsSecondaryPage(
                title = group.title,
                onBack = { selectedGroup = null }
            ) {
                item { AppearanceDetail(state, actions) }
            }

            SettingsGroup.INTERFACE -> SettingsSecondaryPage(
                title = group.title,
                onBack = { selectedGroup = null }
            ) {
                item { InterfaceDetail(state, actions) }
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

            SettingsGroup.GENERAL -> SettingsSecondaryPage(
                title = group.title,
                onBack = { selectedGroup = null }
            ) {
                item { GeneralDetail(state, actions) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMainPage(
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

            val settingSections = listOf(
                "安全与隐私" to listOf(SettingsGroup.SECURITY, SettingsGroup.PRIVACY),
                "显示与外观" to listOf(SettingsGroup.APPEARANCE, SettingsGroup.INTERFACE),
                "交互与操作" to listOf(SettingsGroup.INTERACTION),
                "数据管理" to listOf(SettingsGroup.DATA_MANAGEMENT),
                "通用" to listOf(SettingsGroup.GENERAL)
            )

            settingSections.forEach { (sectionTitle, groups) ->
                item {
                    SettingsGroupTitle(text = sectionTitle)
                }
                item {
                    SettingsRoundedGroup {
                        groups.forEach { group ->
                            navigationSettingsItem(
                                icon = group.icon,
                                title = group.title,
                                subtitle = group.subtitle,
                                onClick = { onGroupClick(group) }
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}