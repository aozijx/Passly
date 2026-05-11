package com.aozijx.passly.features.vault

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.designsystem.model.VaultCardStyle
import com.aozijx.passly.core.designsystem.widgets.EmptyVaultPlaceholder
import com.aozijx.passly.core.designsystem.widgets.SwipeDirection
import com.aozijx.passly.core.designsystem.widgets.SwipeToAction
import com.aozijx.passly.core.designsystem.widgets.createSwipeAction
import com.aozijx.passly.core.designsystem.widgets.handleSwipeAction
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.FieldKey
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.presentation.VaultSummary
import com.aozijx.passly.domain.strategy.EntryTypeStrategyFactory
import com.aozijx.passly.features.main.MainViewModel
import com.aozijx.passly.features.settings.SettingsViewModel
import com.aozijx.passly.features.vault.components.VaultDialogs
import com.aozijx.passly.features.vault.components.entries.VaultCardStyleRegistry
import com.aozijx.passly.features.vault.components.fab.VaultFab
import com.aozijx.passly.features.vault.components.topbar.VaultTopBar
import com.aozijx.passly.features.vault.model.VaultTab
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultContent(
    mainViewModel: MainViewModel,
    activity: FragmentActivity,
    vaultViewModel: VaultViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    onSettingsClick: () -> Unit = {},
    onPlainExportClick: () -> Unit = {},
    onShowDetail: (VaultEntry) -> Unit = {}
) {
    // 使用 lifecycle-aware 的状态订阅
    val context = LocalContext.current
    val items by vaultViewModel.vaultItems.collectAsStateWithLifecycle()
    val isVaultItemsLoading by vaultViewModel.isVaultItemsLoading.collectAsStateWithLifecycle()
    val selectedTab by vaultViewModel.selectedTab.collectAsStateWithLifecycle()
    val visibleTabs by vaultViewModel.visibleTabs.collectAsStateWithLifecycle()
    val totpStates by vaultViewModel.totpStates.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    // 设置相关的 UI 行为开关
    val isSwipeEnabled = settingsUiState.isSwipeEnabled
    val swipeLeftAction = settingsUiState.swipeLeftAction
    val swipeRightAction = settingsUiState.swipeRightAction
    val isStatusBarAutoHide = settingsUiState.isStatusBarAutoHide
    val isTopBarCollapsible = settingsUiState.isTopBarCollapsible
    val isTabBarCollapsible = settingsUiState.isTabBarCollapsible
    val perTypeStyleMap = remember(settingsUiState.cardStyleByEntryType) {
        settingsUiState.cardStyleByEntryType
    }
    var isFabVisible by remember { mutableStateOf(true) }

    val initialTabIndex = visibleTabs.indexOf(selectedTab).coerceAtLeast(0)
    val pagerState =
        rememberPagerState(initialPage = initialTabIndex) { visibleTabs.size.coerceAtLeast(1) }

    // 当前选中的 Tab 若被设置隐藏，自动回退到第一项（通常是 ALL）。
    LaunchedEffect(visibleTabs, selectedTab) {
        if (visibleTabs.isEmpty()) return@LaunchedEffect
        if (selectedTab !in visibleTabs) {
            vaultViewModel.selectTab(visibleTabs.first())
            return@LaunchedEffect
        }
        val targetIndex = visibleTabs.indexOf(selectedTab)
        if (pagerState.settledPage != targetIndex && pagerState.pageCount > targetIndex) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }

    // 同步 Pager -> ViewModel：使用 snapshotFlow 避免以 settledPage 作为 key 重启 effect 导致振荡。
    LaunchedEffect(pagerState, visibleTabs) {
        snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect { page ->
            val newTab = visibleTabs.getOrNull(page) ?: return@collect
            if (newTab != selectedTab) {
                vaultViewModel.selectTab(newTab)
            }
        }
    }

    // 状态栏自动隐藏逻辑（来自旧版）
    LaunchedEffect(scrollBehavior.state.collapsedFraction, isStatusBarAutoHide) {
        if (!isStatusBarAutoHide) {
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.show(WindowInsetsCompat.Type.statusBars())
            return@LaunchedEffect
        }
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        // 增加阈值和平滑处理，防止频繁切换导致手势冲突
        if (scrollBehavior.state.collapsedFraction > 0.6f) {
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        } else if (scrollBehavior.state.collapsedFraction < 0.4f) {
            insetsController.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    // 通用复制逻辑
    val performCopy: (FieldKey, VaultSummary) -> Unit = { fieldKey, item ->
        val strategy = EntryTypeStrategyFactory.getStrategy(item.entryType)
        val label = strategy.getCopyLabel(fieldKey)

        if (fieldKey == FieldKey.PASSWORD && !item.totpSecret.isNullOrBlank()) {
            totpStates[item.id]?.let { state ->
                if (state.code.isNotEmpty() && !state.code.contains("-")) {
                    ClipboardUtils.copy(activity, state.code)
                    Toast.makeText(context, "验证码已复制", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            vaultViewModel.loadEntryById(item.id) { fullEntry ->
                val rawValue = strategy.getFieldValue(fullEntry, fieldKey) ?: return@loadEntryById
                vaultViewModel.decryptSingle(
                    activity = activity,
                    encryptedData = rawValue,
                    authenticate = { act, t, s, _, ok ->
                        mainViewModel.requestAuth(
                            act, t, s, onSuccess = ok
                        )
                    },
                    onResult = { decrypted ->
                        decrypted?.let {
                            ClipboardUtils.copy(activity, it)
                            Toast.makeText(context, "${label}已复制", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
        }
    }

    // 统一的滑动触发处理
    val onSwipeTriggered: (SwipeActionType, VaultSummary) -> Unit = { action, item ->
        handleSwipeAction(
            actionType = action,
            item = item,
            onAuthRequired = { ok ->
                mainViewModel.requestAuth(
                    activity, "安全验证", item.title, onSuccess = ok
                )
            },
            onQuickDelete = { vaultViewModel.quickDelete(it) },
            onCopy = { fieldKey -> performCopy(fieldKey, item) },
            onShowDetail = { vaultViewModel.loadEntryById(item.id) { onShowDetail(it) } })
    }

    // 导出/导入 Launcher
    var pendingManualExportFileName by remember { mutableStateOf<String?>(null) }
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
            it?.let { selectedUri ->
                settingsViewModel.backup.startExport(
                    selectedUri, fileNameHint = pendingManualExportFileName
                )
            }
            pendingManualExportFileName = null
        }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { settingsViewModel.backup.startImport(it) }
    }

    // FAB 滑动隐藏/显示
    val fabScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1f) isFabVisible = false
                else if (available.y > 1f) isFabVisible = true
                return Offset.Zero
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isTopBarCollapsible || isTabBarCollapsible || isStatusBarAutoHide) Modifier.nestedScroll(
                    scrollBehavior.nestedScrollConnection
                )
                else Modifier
            )
            .nestedScroll(fabScrollConnection), topBar = {
            androidx.compose.foundation.layout.Column {
                VaultTopBar(
                    vaultViewModel = vaultViewModel,
                    scrollBehavior = scrollBehavior,
                    onExportClick = {
                        val started = settingsViewModel.backup.tryStartExportInConfiguredDirectory(
                            settingsUiState.backupDirectoryUri
                        )
                        if (!started) {
                            val manualFileName = settingsViewModel.backup.nextBackupFileName()
                            pendingManualExportFileName = manualFileName
                            exportLauncher.launch(manualFileName)
                        }
                    },
                    onPlainJsonExportClick = onPlainExportClick,
                    onImportClick = {
                        importLauncher.launch(
                            arrayOf(
                                "application/octet-stream", "*/*"
                            )
                        )
                    },
                    onSettingsClick = onSettingsClick,
                    isStatusBarAutoHide = isStatusBarAutoHide,
                    isTopBarCollapsible = isTopBarCollapsible,
                    isTabBarCollapsible = isTabBarCollapsible
                )

                if (isVaultItemsLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }, floatingActionButton = {
            VaultFab(
                viewModel = vaultViewModel, isVisible = isFabVisible
            )
        }, contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState, modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { pageIndex ->
            val currentTab = visibleTabs.getOrNull(pageIndex) ?: VaultTab.ALL
            val displayItems = remember(items, currentTab) {
                when (currentTab) {
                    VaultTab.ALL -> items
                    VaultTab.PASSWORDS -> items.filter { it.totpSecret.isNullOrBlank() }
                    VaultTab.TOTP -> items.filter { !it.totpSecret.isNullOrBlank() }
                }
            }

            if (displayItems.isEmpty() && !isVaultItemsLoading) {
                EmptyVaultPlaceholder()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = displayItems, key = { it.id }) { item ->
                        val cardStyle =
                            perTypeStyleMap[item.entryType]?.takeIf { it != VaultCardStyle.DEFAULT }
                                ?: VaultCardStyle.resolveForEntryType(
                                    settingsUiState.cardStyle, item.entryType
                                )

                        val actions = listOfNotNull(
                            createSwipeAction(
                                actionType = swipeLeftAction,
                                direction = SwipeDirection.LEFT,
                                onAction = { onSwipeTriggered(swipeLeftAction, item) },
                                backgroundColor = if (swipeLeftAction == SwipeActionType.DELETE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                iconTint = Color.White
                            ), createSwipeAction(
                                actionType = swipeRightAction,
                                direction = SwipeDirection.RIGHT,
                                onAction = { onSwipeTriggered(swipeRightAction, item) },
                                backgroundColor = if (swipeRightAction == SwipeActionType.DELETE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                iconTint = Color.White
                            )
                        )

                        SwipeToAction(
                            actions = actions,
                            modifier = Modifier.fillMaxWidth(),
                            isActive = isSwipeEnabled,
                        ) {
                            VaultCardStyleRegistry.RenderVaultItem(
                                style = cardStyle, entry = item, viewModel = vaultViewModel
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
            }
        }
    }

    VaultDialogs(
        mainViewModel = mainViewModel,
        activity = activity,
        vaultViewModel = vaultViewModel,
        settingsViewModel = settingsViewModel
    )
}