package com.aozijx.passly.features.vault.components.topbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
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

    // 重置子菜单状态
    LaunchedEffect(expanded) {
        if (!expanded) {
            showCategorySubMenu = false
            categorySearchQuery = ""
            isCategorySearchVisible = false
        }
    }

    // 自动聚焦逻辑
    LaunchedEffect(isCategorySearchVisible) {
        if (isCategorySearchVisible) {
            categoryFocusRequester.requestFocus()
        }
    }

    val filteredCategories = remember(availableCategories, categorySearchQuery) {
        if (categorySearchQuery.isBlank()) availableCategories
        else availableCategories.filter { it.contains(categorySearchQuery, ignoreCase = true) }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .widthIn(min = 140.dp)
            .animateContentSize()
    ) {
        // 使用 AnimatedContent 实现线性滑动过渡
        AnimatedContent(
            targetState = showCategorySubMenu, transitionSpec = {
                val transition = if (targetState) {
                    // 进入子菜单：从右推入
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                    // 返回主菜单：向左向右拉出
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut())
                }
                transition.using(SizeTransform(clip = false))
            }, label = "MenuPageTransition"
        ) { isSubMenu ->
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isSubMenu) {
                    // --- 主菜单内容 ---
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.vault_menu_filter)) },
                        onClick = { showCategorySubMenu = true },
                        leadingIcon = { Icon(Icons.Default.FilterList, null) })
                    DropdownMenuItem(
                        text = { Text(stringResource(if (showTOTPCode) R.string.vault_menu_hide_totp else R.string.vault_menu_show_totp)) },
                        onClick = {
                            onToggleTotpVisibility()
                            onDismissRequest()
                        },
                        leadingIcon = {
                            Icon(
                                if (showTOTPCode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null
                            )
                        })

                    if (!isAutofillEnabled) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.vault_menu_enable_autofill)) },
                            onClick = {
                                onEnableAutofillClick()
                                onDismissRequest()
                            },
                            leadingIcon = { Icon(Icons.Default.SettingsSuggest, null) })
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_settings)) },
                        onClick = {
                            onDismissRequest()
                            onSettingsClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Settings, null) })
                    CustomExportMenuItem(
                        text = stringResource(R.string.vault_menu_export),
                        leadingIcon = { Icon(Icons.Default.FileUpload, null) },
                        onClick = {
                            onDismissRequest()
                            onExportClick()
                        },
                        onLongClick = {
                            onDismissRequest()
                            onOpenPlainExport()
                        })
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.vault_menu_import)) },
                        onClick = {
                            onDismissRequest()
                            onImportClick()
                        },
                        leadingIcon = { Icon(Icons.Default.FileDownload, null) })
                } else {
                    // --- 分类子菜单内容 ---
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_back)) },
                        onClick = {
                            if (isCategorySearchVisible) {
                                isCategorySearchVisible = false
                                categorySearchQuery = ""
                            } else {
                                showCategorySubMenu = false
                            }
                        },
                        leadingIcon = {
                            Icon(
                                if (isCategorySearchVisible) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                null
                            )
                        })

                    AnimatedVisibility(
                        visible = isCategorySearchVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
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
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
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

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.vault_menu_all_categories)) },
                        onClick = {
                            onCategorySelected(null)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .then(
                                if (selectedCategory == null) {
                                    Modifier.background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.small
                                    )
                                } else Modifier
                            )
                    )

                    filteredCategories.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    category,
                                    color = if (selectedCategory == category) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }, onClick = {
                                onCategorySelected(category)
                                onDismissRequest()
                            }, modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .then(
                                    if (selectedCategory == category) {
                                        Modifier.background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = MaterialTheme.shapes.small
                                        )
                                    } else Modifier
                                )
                        )
                    }

                    if (filteredCategories.isEmpty() && categorySearchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "无匹配分类",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
