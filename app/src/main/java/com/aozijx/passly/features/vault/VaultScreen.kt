package com.aozijx.passly.features.vault

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aozijx.passly.AppContext
import com.aozijx.passly.MainViewModel
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.designsystem.model.AddType
import com.aozijx.passly.core.designsystem.model.VaultCardStyle
import com.aozijx.passly.core.designsystem.model.VaultTab
import com.aozijx.passly.core.designsystem.widgets.EmptyVaultPlaceholder
import com.aozijx.passly.core.designsystem.widgets.SwipeDirection
import com.aozijx.passly.core.designsystem.widgets.SwipeToAction
import com.aozijx.passly.core.designsystem.widgets.createSwipeAction
import com.aozijx.passly.core.designsystem.widgets.handleSwipeAction
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.features.scanner.VaultScanner
import com.aozijx.passly.features.settings.SettingsViewModel
import com.aozijx.passly.features.vault.components.VaultDialogs
import com.aozijx.passly.features.vault.components.entries.VaultCardStyleRegistry
import com.aozijx.passly.features.vault.components.fab.VaultFab
import com.aozijx.passly.features.vault.components.topbar.VaultTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultContent(
    activity: FragmentActivity,
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    onSettingsClick: () -> Unit = {},
    onShowDetail: (VaultEntry) -> Unit = {}
) {
    val items by vaultViewModel.vaultItems.collectAsState()
    val isVaultItemsLoading by vaultViewModel.isVaultItemsLoading.collectAsState()
    val selectedTab by vaultViewModel.selectedTab.collectAsState()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val vaultPrefs = remember { AppContext.get().preference }
    val isSwipeEnabled by vaultPrefs.isSwipeEnabled.collectAsState(initial = true)
    val swipeLeftAction by vaultPrefs.swipeLeftAction.collectAsState(initial = SwipeActionType.DELETE)
    val swipeRightAction by vaultPrefs.swipeRightAction.collectAsState(initial = SwipeActionType.DISABLED)
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val backupDirectoryUri = settingsUiState.backupDirectoryUri

    // 沉浸式设置状态
    val isStatusBarAutoHide = settingsUiState.isStatusBarAutoHide
    val isTopBarCollapsible = settingsUiState.isTopBarCollapsible
    val isTabBarCollapsible = settingsUiState.isTabBarCollapsible
    val perTypeStyleMap = settingsUiState.cardStyleByEntryType

    var isFabVisible by remember { mutableStateOf(true) }
    val pagerState = rememberPagerState(initialPage = selectedTab.ordinal) { VaultTab.entries.size }

    // 处理快捷方式进入扫码的逻辑
    LaunchedEffect(Unit) {
        val intent = activity.intent
        if (intent?.getBooleanExtra("START_SCAN", false) == true) {
            vaultViewModel.addType = AddType.SCAN
            // 消费掉这个标志，防止旋转屏幕等操作再次触发
            intent.removeExtra("START_SCAN")
        }
    }

    val fabScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1) isFabVisible = false else if (available.y > 1) isFabVisible =
                    true
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab.ordinal) {
            pagerState.scrollToPage(selectedTab.ordinal)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val newTab = VaultTab.entries[pagerState.currentPage]
        if (newTab != selectedTab) {
            vaultViewModel.selectTab(newTab)
        }
    }

    // 处理状态栏隐藏逻辑
    LaunchedEffect(scrollBehavior.state.collapsedFraction, isStatusBarAutoHide) {
        if (!isStatusBarAutoHide) {
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.show(WindowInsetsCompat.Type.statusBars())
            return@LaunchedEffect
        }
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        // 增加阈值并添加平滑处理，防止频繁切换导致手势冲突
        if (scrollBehavior.state.collapsedFraction > 0.6f) {
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        } else if (scrollBehavior.state.collapsedFraction < 0.4f) {
            insetsController.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    var pendingManualExportFileName by remember { mutableStateOf<String?>(null) }
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
            it?.let { selectedUri ->
                settingsViewModel.startExport(
                    selectedUri, fileNameHint = pendingManualExportFileName
                )
            }
            pendingManualExportFileName = null
        }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { settingsViewModel.startImport(it) }
    }

    LaunchedEffect(settingsViewModel.backupMessage) {
        settingsViewModel.backupMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            settingsViewModel.clearBackupMessage()
        }
    }

    LaunchedEffect(settingsViewModel.backupExportFallbackFileName) {
        val fallbackName = settingsViewModel.backupExportFallbackFileName ?: return@LaunchedEffect
        pendingManualExportFileName = fallbackName
        exportLauncher.launch(fallbackName)
        settingsViewModel.consumeBackupExportFallbackFileName()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            // 核心修复：只有在至少有一个组件需要折叠时，才挂载 scrollBehavior 的 nestedScrollConnection
            modifier = Modifier
                .then(
                    if (isTopBarCollapsible || isTabBarCollapsible || isStatusBarAutoHide) Modifier.nestedScroll(
                        scrollBehavior.nestedScrollConnection
                    ) else Modifier
                )
                .nestedScroll(fabScrollConnection),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                VaultTopBar(
                    vaultViewModel = vaultViewModel,
                    scrollBehavior = scrollBehavior,
                    onExportClick = {
                        val startedFromConfiguredDirectory =
                            settingsViewModel.tryStartExportInConfiguredDirectory(
                                backupDirectoryUri
                            )
                        if (!startedFromConfiguredDirectory) {
                            val manualFileName = settingsViewModel.nextBackupFileName()
                            pendingManualExportFileName = manualFileName
                            exportLauncher.launch(manualFileName)
                        }
                    },
                    onImportClick = {
                        importLauncher.launch(
                            arrayOf(
                                "application/octet-stream", "*/*"
                            )
                        )
                    },
                    onSettingsClick = onSettingsClick,
                    isTopBarCollapsible = isTopBarCollapsible,
                    isTabBarCollapsible = isTabBarCollapsible,
                    isStatusBarAutoHide = isStatusBarAutoHide
                )
            },
            floatingActionButton = {
                VaultFab(
                    viewModel = vaultViewModel, isVisible = isFabVisible
                )
            }) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surface)
            ) { pageIndex ->
                val currentTab = VaultTab.entries[pageIndex]
                val listState = rememberLazyListState()
                val filteredItems = when (currentTab) {
                    VaultTab.ALL -> items
                    VaultTab.PASSWORDS -> items.filter { it.totpSecret.isNullOrBlank() }
                    VaultTab.TOTP -> items.filter { !it.totpSecret.isNullOrBlank() }
                }

                if (isVaultItemsLoading) {
                    VaultListSkeleton()
                } else if (filteredItems.isEmpty()) {
                    EmptyVaultPlaceholder()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            val selectedStyle =
                                perTypeStyleMap[item.entryType] ?: VaultCardStyle.DEFAULT
                            val resolvedStyle =
                                VaultCardStyle.resolveForEntryType(selectedStyle, item.entryType)
                            val itemContent = @Composable {
                                VaultCardStyleRegistry.RenderVaultItem(
                                    style = resolvedStyle, entry = item, viewModel = vaultViewModel
                                )
                            }

                            if (isSwipeEnabled) {
                                val leftColors = swipeRevealColors(swipeLeftAction)
                                val leftAction = createSwipeAction(
                                    actionType = swipeLeftAction,
                                    direction = SwipeDirection.LEFT,
                                    onAction = {
                                        handleSwipeAction(
                                            swipeLeftAction,
                                            item,
                                            onAuthRequired = { onSuccess ->
                                                mainViewModel.authenticate(
                                                    activity,
                                                    "确认验证",
                                                    item.title,
                                                    null,
                                                    onSuccess
                                                )
                                            },
                                            onQuickDelete = { vaultViewModel.quickDelete(it) },
                                            onCopyPassword = { password ->
                                                ClipboardUtils.copy(activity, password)
                                                Toast.makeText(
                                                    context, "已复制", Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            onDecryptPassword = { callback ->
                                                vaultViewModel.loadEntryById(item.id) { fullEntry ->
                                                    vaultViewModel.decryptSingle(
                                                        activity = activity,
                                                        encryptedData = fullEntry.password,
                                                        authenticate = { act, title, subtitle, _, onSuccess ->
                                                            mainViewModel.authenticate(
                                                                act,
                                                                title,
                                                                subtitle,
                                                                null,
                                                                onSuccess
                                                            )
                                                        },
                                                        onResult = { callback(it) })
                                                }
                                            },
                                            onShowDetail = {
                                                vaultViewModel.loadEntryById(item.id) { entry ->
                                                    onShowDetail(entry)
                                                }
                                            },
                                            onShowEditDetail = {
                                                vaultViewModel.loadEntryById(item.id) { entry ->
                                                    vaultViewModel.showDetailForEdit(entry)
                                                }
                                            })
                                    },
                                    backgroundColor = leftColors.first,
                                    iconTint = leftColors.second
                                )
                                val rightColors = swipeRevealColors(swipeRightAction)
                                val rightAction = createSwipeAction(
                                    actionType = swipeRightAction,
                                    direction = SwipeDirection.RIGHT,
                                    onAction = {
                                        handleSwipeAction(
                                            swipeRightAction,
                                            item,
                                            onAuthRequired = { onSuccess ->
                                                mainViewModel.authenticate(
                                                    activity,
                                                    "确认验证",
                                                    item.title,
                                                    null,
                                                    onSuccess
                                                )
                                            },
                                            onQuickDelete = { vaultViewModel.quickDelete(it) },
                                            onCopyPassword = { password ->
                                                ClipboardUtils.copy(activity, password)
                                                Toast.makeText(
                                                    context, "已复制", Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            onDecryptPassword = { callback ->
                                                vaultViewModel.loadEntryById(item.id) { fullEntry ->
                                                    vaultViewModel.decryptSingle(
                                                        activity = activity,
                                                        encryptedData = fullEntry.password,
                                                        authenticate = { act, title, subtitle, _, onSuccess ->
                                                            mainViewModel.authenticate(
                                                                act,
                                                                title,
                                                                subtitle,
                                                                null,
                                                                onSuccess
                                                            )
                                                        },
                                                        onResult = { callback(it) })
                                                }
                                            },
                                            onShowDetail = {
                                                vaultViewModel.loadEntryById(item.id) { entry ->
                                                    onShowDetail(entry)
                                                }
                                            },
                                            onShowEditDetail = {
                                                vaultViewModel.loadEntryById(item.id) { entry ->
                                                    vaultViewModel.showDetailForEdit(entry)
                                                }
                                            })
                                    },
                                    backgroundColor = rightColors.first,
                                    iconTint = rightColors.second
                                )
                                val actions = listOfNotNull(leftAction, rightAction)
                                if (actions.isNotEmpty()) {
                                    SwipeToAction(
                                        actions = actions,
                                        isActive = vaultViewModel.itemToDelete?.id != item.id
                                    ) {
                                        itemContent()
                                    }
                                } else {
                                    itemContent()
                                }
                            } else {
                                itemContent()
                            }
                        }
                        item {
                            Spacer(
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .height(80.dp)
                            )
                        }
                    }
                }
            }

            VaultDialogs(
                activity = activity,
                mainViewModel = mainViewModel,
                vaultViewModel = vaultViewModel,
                settingsViewModel = settingsViewModel
            )
        }

        AnimatedVisibility(
            visible = vaultViewModel.addType == AddType.SCAN,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
            ) {
                VaultScanner(
                    vaultViewModel = vaultViewModel,
                    onDismiss = { vaultViewModel.addType = AddType.NONE })
            }
        }
    }
}

@Composable
private fun VaultListSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(6) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {}
        }
        item {
            Spacer(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(80.dp)
            )
        }
    }
}

@Composable
private fun swipeRevealColors(actionType: SwipeActionType): Pair<Color, Color> {
    return when (actionType) {
        SwipeActionType.DELETE -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        SwipeActionType.COPY_PASSWORD -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        SwipeActionType.EDIT -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        SwipeActionType.DETAIL -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        SwipeActionType.DISABLED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
}
