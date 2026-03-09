package com.example.poop.ui.screens.vault.components.layout

import android.content.Intent
import android.provider.Settings
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.poop.R
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.core.VaultTab
import com.example.poop.util.Logcat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(
    viewModel: VaultViewModel,
    scrollBehavior: TopAppBarScrollBehavior,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val focusManager = LocalFocusManager.current

    // 预解析字符串资源，避免在 onClick 等非 Composable 环境中直接查询导致的警告和错误
    val autofillToastMessage = stringResource(R.string.vault_toast_enable_autofill_manual)

    var showCategorySubMenu by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.isMoreMenuExpanded) {
        if (!viewModel.isMoreMenuExpanded) {
            showCategorySubMenu = false
        }
    }

    Column {
        CenterAlignedTopAppBar(
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets.statusBars,
            title = {
                if (viewModel.isSearchActive) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.vault_search_placeholder)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (selectedCategory != null) {
                                stringResource(R.string.vault_title_category, selectedCategory!!)
                            } else {
                                stringResource(R.string.vault_title_default)
                            },
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedCategory != null) {
                            IconButton(onClick = { viewModel.onCategorySelect(null) }) {
                                Icon(
                                    Icons.Default.Clear, 
                                    stringResource(R.string.vault_clear_filter), 
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.toggleSearch(!viewModel.isSearchActive) }) {
                    Icon(
                        if (viewModel.isSearchActive) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Search,
                        contentDescription = if (viewModel.isSearchActive) {
                            stringResource(R.string.action_back)
                        } else {
                            stringResource(R.string.action_search)
                        }
                    )
                }
            },
            actions = {
                if (!viewModel.isSearchActive) {
                    Box {
                        IconButton(onClick = { viewModel.isMoreMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more))
                        }
                        DropdownMenu(
                            expanded = viewModel.isMoreMenuExpanded,
                            onDismissRequest = { viewModel.isMoreMenuExpanded = false }
                        ) {
                            if (!showCategorySubMenu) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.vault_menu_filter)) },
                                    onClick = { showCategorySubMenu = true },
                                    leadingIcon = { Icon(Icons.Default.FilterList, null) }
                                )

                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            if (viewModel.showTOTPCode) stringResource(R.string.vault_menu_hide_totp) 
                                            else stringResource(R.string.vault_menu_show_totp)
                                        ) 
                                    },
                                    onClick = {
                                        viewModel.showTOTPCode = !viewModel.showTOTPCode
                                        viewModel.isMoreMenuExpanded = false
                                    },
                                    leadingIcon = { 
                                        Icon(
                                            if (viewModel.showTOTPCode) Icons.Default.VisibilityOff else Icons.Default.Visibility, 
                                            null
                                        ) 
                                    }
                                )

                                val autofillManager = remember { context.getSystemService(AutofillManager::class.java) }
                                val isAutofillEnabled = remember(autofillManager) {
                                    autofillManager?.isAutofillSupported == true && autofillManager.hasEnabledAutofillServices()
                                }
                                
                                if (!isAutofillEnabled) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.vault_menu_enable_autofill)) },
                                        onClick = {
                                            viewModel.isMoreMenuExpanded = false
                                            // 优先级 1：标准的自动填充请求（最推荐）
                                            val standardIntent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                                                data = "package:${context.packageName}".toUri()
                                            }
                                            // 定义一个简单的跳转方法
                                            fun tryStartActivity(intent: Intent): Boolean {
                                                return try {
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    context.startActivity(intent)
                                                    true
                                                } catch (e: Exception) {
                                                    Logcat.e("VaultTopBar", autofillToastMessage,e)
                                                    false
                                                }
                                            }

                                            if (!tryStartActivity(standardIntent)) {
                                                // 优先级 2：如果标准方式报错，尝试进入“默认应用”设置（鸿蒙/国产机最稳妥的二级入口）
                                                val defaultAppsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)

                                                if (!tryStartActivity(defaultAppsIntent)) {
                                                    // 优先级 3：终极兜底，只跳到总设置，并给提示
                                                    tryStartActivity(Intent(Settings.ACTION_SETTINGS))
                                                    Toast.makeText(context, autofillToastMessage, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Default.SettingsSuggest, null) }
                                    )
                                }

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.vault_menu_export)) },
                                    onClick = {
                                        viewModel.isMoreMenuExpanded = false
                                        onExportClick()
                                    },
                                    leadingIcon = { Icon(Icons.Default.FileUpload, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.vault_menu_import)) },
                                    onClick = {
                                        viewModel.isMoreMenuExpanded = false
                                        onImportClick()
                                    },
                                    leadingIcon = { Icon(Icons.Default.FileDownload, null) }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_back)) },
                                    onClick = { showCategorySubMenu = false },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.vault_menu_all_categories)) },
                                    onClick = {
                                        viewModel.onCategorySelect(null)
                                        viewModel.isMoreMenuExpanded = false
                                    },
                                    trailingIcon = { if (selectedCategory == null) Icon(Icons.Default.Check, null) }
                                )

                                availableCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = {
                                            viewModel.onCategorySelect(category)
                                            viewModel.isMoreMenuExpanded = false
                                        },
                                        trailingIcon = { if (selectedCategory == category) Icon(Icons.Default.Check, null) }
                                    )
                                }
                            }
                        }
                    }
                } else if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.vault_clear_filter))
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            )
        )

        AnimatedVisibility(
            visible = !viewModel.isSearchActive && selectedCategory == null && scrollBehavior.state.collapsedFraction < 0.5f,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SecondaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(selectedTab.ordinal),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                VaultTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.onTabSelect(tab) },
                        text = {
                            Text(
                                when (tab) {
                                    VaultTab.ALL -> stringResource(R.string.vault_tab_all)
                                    VaultTab.PASSWORDS -> stringResource(R.string.vault_tab_passwords)
                                    VaultTab.TOTP -> stringResource(R.string.vault_tab_totp)
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }
    }
}
