package com.aozijx.passly.feature.settings.shell

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.feature.settings.internal.SettingsGroup
import com.aozijx.passly.feature.settings.navigation.SettingsRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsMainPage(
    onBack: () -> Unit,
    onUpdateInteraction: () -> Unit,
    onGroupClick: (SettingsRoute) -> Unit
) {
    Scaffold(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onUpdateInteraction
            ),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(com.aozijx.passly.R.string.settings),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(com.aozijx.passly.R.string.back)
                        )
                    }
                }
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

            val sections = SettingsGroup.entries.groupBy { it.sectionTitleRes }

            sections.forEach { (sectionTitleRes, groups) ->
                item {
                    SettingsSectionTitle(text = stringResource(sectionTitleRes))
                }
                item {
                    RoundedGroup(
                        items = groups.map { group ->
                            navigationSettingsGroupItem(
                                key = group.route.route,
                                icon = group.icon,
                                title = stringResource(group.titleRes),
                                subtitle = stringResource(group.subtitleRes),
                                onClick = { onGroupClick(group.route) }
                            )
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}
