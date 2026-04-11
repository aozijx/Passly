package com.aozijx.passly.features.vault.components.topbar.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R

@Composable
fun VaultDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    showTOTPCode: Boolean,
    onToggleTotpVisibility: () -> Unit,
    isAutofillEnabled: Boolean,
    onEnableAutofillClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExportClick: () -> Unit,
    onOpenPlainExport: () -> Unit,
    onImportClick: () -> Unit,
    availableCategories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    var showCategorySubMenu by remember { mutableStateOf(false) }
    var categorySearchQuery by remember { mutableStateOf("") }
    var isCategorySearchVisible by remember { mutableStateOf(false) }
    val categoryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(expanded) {
        if (!expanded) {
            showCategorySubMenu = false
            categorySearchQuery = ""
            isCategorySearchVisible = false
        }
    }

    LaunchedEffect(isCategorySearchVisible) {
        if (isCategorySearchVisible) categoryFocusRequester.requestFocus()
    }

    val filteredCategories = remember(availableCategories, categorySearchQuery) {
        if (categorySearchQuery.isBlank()) availableCategories
        else availableCategories.filter { it.contains(categorySearchQuery, ignoreCase = true) }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .widthIn(min = 150.dp)
            .animateContentSize()
    ) {
        AnimatedContent(
            targetState = showCategorySubMenu, transitionSpec = {
                val transition = if (targetState) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                }
                transition.using(SizeTransform(clip = false))
            }, label = "MenuPageTransition"
        ) { isSubMenu ->
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!isSubMenu) {
                    // --- 主菜单内容 ---
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.vault_menu_filter)) },
                        onClick = { showCategorySubMenu = true },
                        leadingIcon = { Icon(Icons.Default.FilterList, null) })
                    DropdownMenuItem(
                        text = { Text(stringResource(if (showTOTPCode) R.string.vault_menu_hide_totp else R.string.vault_menu_show_totp)) },
                        onClick = { onToggleTotpVisibility(); onDismissRequest() },
                        leadingIcon = {
                            Icon(
                                if (showTOTPCode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null
                            )
                        })
                    if (!isAutofillEnabled) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.vault_menu_enable_autofill)) },
                            onClick = { onEnableAutofillClick(); onDismissRequest() },
                            leadingIcon = { Icon(Icons.Default.SettingsSuggest, null) })
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_settings)) },
                        onClick = { onDismissRequest(); onSettingsClick() },
                        leadingIcon = { Icon(Icons.Default.Settings, null) })
                    CustomExportMenuItem(
                        text = stringResource(R.string.vault_menu_export),
                        leadingIcon = { Icon(Icons.Default.FileUpload, null) },
                        onClick = { onDismissRequest(); onExportClick() },
                        onLongClick = { onDismissRequest(); onOpenPlainExport() })
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.vault_menu_import)) },
                        onClick = { onDismissRequest(); onImportClick() },
                        leadingIcon = { Icon(Icons.Default.FileDownload, null) })
                } else {
                    // --- 分类子菜单内容 ---
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_back)) },
                        onClick = {
                            if (isCategorySearchVisible) {
                                isCategorySearchVisible = false; categorySearchQuery = ""
                            } else showCategorySubMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                if (isCategorySearchVisible) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                null
                            )
                        })

                    AnimatedVisibility(visible = isCategorySearchVisible) {
                        OutlinedTextField(
                            value = categorySearchQuery,
                            onValueChange = { categorySearchQuery = it },
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .fillMaxWidth()
                                .focusRequester(categoryFocusRequester),
                            placeholder = {
                                Text(
                                    "搜索分类...", style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search, null, modifier = Modifier.size(16.dp)
                                )
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.3f
                                )
                            )
                        )
                    }

                    if (!isCategorySearchVisible) {
                        DropdownMenuItem(
                            text = { Text("搜索分类") },
                            onClick = { isCategorySearchVisible = true },
                            leadingIcon = { Icon(Icons.Default.Search, null) })
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // 渲染“全部分类”项
                    val isAllSelected = selectedCategory == null
                    DropdownMenuItem(
                        text = {
                        Text(
                            text = stringResource(R.string.vault_menu_all_categories),
                            color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                        onClick = { onCategorySelected(null); onDismissRequest() },
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(if (isAllSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    )

                    filteredCategories.forEach { category ->
                        val isSelected = selectedCategory == category
                        DropdownMenuItem(
                            text = {
                            Text(
                                text = category,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                            onClick = { onCategorySelected(category); onDismissRequest() },
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}
