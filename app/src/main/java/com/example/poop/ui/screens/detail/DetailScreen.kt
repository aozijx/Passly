package com.example.poop.ui.screens.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.poop.ui.navigation.Screen
import com.example.poop.ui.navigation.TopBarConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(navController: NavHostController) {
    val context = LocalContext.current
    val systemInfoManager = remember { SystemInfoManager(context) }
    val categoryGroups = remember {
        SystemInfoManager.INFO_ITEMS
            .groupBy { it.category }
            .toList()
    }

    TopBarConfig(
        title = "设备详情",
        centerTitle = true, actions = {
            IconButton(onClick = { /* TODO: viewModel.onSearchClick() */ }) {
                Icon(Icons.Default.Search, contentDescription = "搜索")
            }
        }
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AnalysisEntryCard(onClick = { navController.navigate(Screen.AppAnalysis.route) })
            }

            categoryGroups.forEach { (category, items) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CategoryTitle(category)
                }

                items(items) { infoItem ->
                    DetailItem(
                        title = infoItem.title,
                        value = infoItem.valueProvider(systemInfoManager)
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisEntryCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        // 使用 primaryContainer 让入口在视觉上比其他普通项更突出
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    null,
                    modifier = Modifier.padding(10.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "应用架构与 SDK 分析",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "查看 API 级别与 64 位支持",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun CategoryTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary, // 分组标题使用主色调
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(top = 20.dp, bottom = 8.dp, start = 4.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Start
    )
}

@Composable
fun DetailItem(title: String, value: String) {
    Card(
        modifier = Modifier.padding(vertical = 4.dp),
        // 普通项使用 surfaceVariant，既能区分背景，又不会过于显眼
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary, // 标签使用次级色
                fontSize = 11.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface, // 数值使用强调色
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 1
            )
        }
    }
}
