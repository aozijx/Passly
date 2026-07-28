package com.aozijx.passly.feature.settings.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.ui.adaptive.LocalPasslyAdaptiveLayout
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.feature.settings.internal.SettingsGroup
import com.aozijx.passly.feature.settings.navigation.SettingsRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsMainPage(
    onBack: () -> Unit,
    onGroupClick: (SettingsRoute) -> Unit
) {
    val adaptiveLayout = LocalPasslyAdaptiveLayout.current

    Scaffold(
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
        val sections = SettingsGroup.entries.groupBy { it.sectionTitleRes }.toList()
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (adaptiveLayout.isExpanded) 2 else 1),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (adaptiveLayout.isAtLeastMedium) 32.dp else 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                end = if (adaptiveLayout.isAtLeastMedium) 32.dp else 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sections, key = { it.first }) { (sectionTitleRes, groups) ->
                Column {
                    SettingsSectionTitle(text = stringResource(sectionTitleRes))
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
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
