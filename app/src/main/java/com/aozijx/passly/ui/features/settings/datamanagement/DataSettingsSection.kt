package com.aozijx.passly.ui.features.settings.datamanagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.ui.features.settings.components.GroupCard
import com.aozijx.passly.ui.features.settings.components.navigationSettingsItem
import com.aozijx.passly.ui.features.settings.components.switchSettingsItem
import com.aozijx.passly.ui.features.settings.shell.SettingsGroupTitle
import com.aozijx.passly.ui.features.settings.shell.SettingsRoundedGroup

@Composable
fun DataSettingsSection(
    isAutoDownloadIcons: Boolean,
    faviconDownloadWhitelist: String,
    onAutoDownloadIconsChange: (Boolean) -> Unit,
    onFaviconWhitelistChange: (String) -> Unit
) {
    val downloadIconsTitle = stringResource(R.string.settings_download_icons)
    val downloadIconsSubtitle = stringResource(R.string.settings_download_icons_subtitle)
    val whitelistTitle = stringResource(R.string.settings_favicon_whitelist)
    val whitelistItems = faviconDownloadWhitelist.split("\n").filter { it.isNotBlank() }
    val whitelistSubtitle = if (whitelistItems.isEmpty()) {
        stringResource(R.string.settings_favicon_whitelist_expand_hint)
    } else {
        stringResource(R.string.settings_favicon_whitelist_item_count, whitelistItems.size)
    }

    var isWhitelistExpanded by remember { mutableStateOf(false) }
    
    SettingsGroupTitle(text = "数据与下载")
    SettingsRoundedGroup {
        switchSettingsItem(
            icon = Icons.Default.CloudDownload,
            title = downloadIconsTitle,
            subtitle = downloadIconsSubtitle,
            checked = isAutoDownloadIcons,
            onCheckedChange = onAutoDownloadIconsChange
        )

        navigationSettingsItem(
            icon = Icons.Default.CloudDownload,
            title = whitelistTitle,
            subtitle = whitelistSubtitle,
            onClick = { isWhitelistExpanded = !isWhitelistExpanded }
        )

        item(visible = isWhitelistExpanded) { position ->
            GroupCard(position = position, contentPadding = PaddingValues(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_favicon_whitelist_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FaviconWhitelistEditor(
                    initialList = whitelistItems,
                    onListChanged = { onFaviconWhitelistChange(it.joinToString("\n")) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaviconWhitelistEditor(
    initialList: List<String> = emptyList(),
    onListChanged: (List<String>) -> Unit
) {
    val items = remember { mutableStateListOf<String>().apply { addAll(initialList) } }
    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.settings_favicon_whitelist_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    addItem(inputText, items) {
                        inputText = ""
                        onListChanged(items)
                    }
                }
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        )

        if (items.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { domain ->
                    AssistChip(
                        onClick = {},
                        label = { Text(domain) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    items.remove(domain)
                                    onListChanged(items)
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "移除",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}


// 辅助添加逻辑（含防呆校验）
private fun addItem(input: String, list: MutableList<String>, onAdded: () -> Unit) {
    val trimmed = input.trim()
    when {
        trimmed.isEmpty() -> return // 空内容忽略
        list.contains(trimmed) -> return // 防重复
        else -> {
            list.add(trimmed)
            onAdded()
        }
    }
}