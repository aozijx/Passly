package com.example.poop.ui.screens.vault

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.poop.ui.screens.vault.components.AddVaultItemDialog
import com.example.poop.ui.screens.vault.components.BackupPasswordDialog
import com.example.poop.ui.screens.vault.components.EmptyVaultPlaceholder
import com.example.poop.ui.screens.vault.components.VaultDetailDialog
import com.example.poop.ui.screens.vault.components.VaultItemRow
import com.example.poop.util.BiometricHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultContent(activity: FragmentActivity, viewModel: VaultViewModel) {
    val items by viewModel.vaultItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val context = LocalContext.current

    val focusManager = LocalFocusManager.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // 文件导出器
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { viewModel.startExport(it) }
    }

    // 文件导入器
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.startImport(it) }
    }

    // 监听备份消息提示
    LaunchedEffect(viewModel.backupMessage) {
        viewModel.backupMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearBackupMessage()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    if (viewModel.isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("搜索密码项...") },
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
                        Text("安全保险箱", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    if (viewModel.isSearchActive) {
                        IconButton(onClick = {
                            viewModel.toggleSearch(false)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    } else {
                        // 将搜索按钮放到左侧
                        IconButton(onClick = { viewModel.toggleSearch(true) }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                    }
                },
                actions = {
                    if (!viewModel.isSearchActive) {
                        // 过滤菜单
                        Box {
                            IconButton(onClick = { viewModel.toggleFilterMenu(true) }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "过滤",
                                    tint = if (selectedCategory != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = viewModel.isFilterMenuExpanded,
                                onDismissRequest = { viewModel.toggleFilterMenu(false) }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("全部") },
                                    onClick = { viewModel.onCategorySelect(null) },
                                    leadingIcon = {
                                        if (selectedCategory == null) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                                availableCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = { viewModel.onCategorySelect(category) },
                                        leadingIcon = {
                                            if (selectedCategory == category) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // 更多菜单（备份与恢复）
                        Box {
                            IconButton(onClick = { viewModel.toggleMoreMenu(true) }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(
                                expanded = viewModel.isMoreMenuExpanded,
                                onDismissRequest = { viewModel.toggleMoreMenu(false) }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("导出备份") },
                                    onClick = {
                                        viewModel.toggleMoreMenu(false)
                                        exportLauncher.launch("vault_backup_${System.currentTimeMillis()}.poop")
                                    },
                                    leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("导入恢复") },
                                    onClick = {
                                        viewModel.toggleMoreMenu(false)
                                        importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                                    },
                                    leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) }
                                )
                            }
                        }
                    } else {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = scrollBehavior.state.collapsedFraction < 0.5f,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                FloatingActionButton(
                    onClick = { viewModel.onAddClick() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.surface)
        ) {
            if (items.isEmpty()) {
                EmptyVaultPlaceholder()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        VaultItemRow(item = item, viewModel = viewModel)
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            // 详情弹窗
            viewModel.detailItem?.let { item ->
                VaultDetailDialog(activity = activity, item = item, viewModel = viewModel)
            }

            // 删除确认
            viewModel.itemToDelete?.let { item ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissDeleteDialog() },
                    title = { Text("确认删除") },
                    text = { Text("确定要删除 \"${item.title}\" 吗？此操作不可撤销。") },
                    confirmButton = {
                        TextButton(onClick = {
                            BiometricHelper.authenticate(
                                activity = activity,
                                title = "确认删除",
                                subtitle = "验证身份以确认删除",
                                onSuccess = {
                                    viewModel.confirmDelete()
                                }
                            )
                        }) {
                            Text("确认删除", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissDeleteDialog() }) { Text("取消") }
                    }
                )
            }

            // 备份密码输入对话框
            if (viewModel.showBackupPasswordDialog) {
                BackupPasswordDialog(activity = activity, viewModel = viewModel)
            }

            // 全局加载遮罩
            if (viewModel.isBackupLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (viewModel.isExporting) "正在导出备份..." else "正在导入恢复...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (viewModel.showAddDialog) {
            AddVaultItemDialog(activity = activity, viewModel = viewModel)
        }
    }
}
