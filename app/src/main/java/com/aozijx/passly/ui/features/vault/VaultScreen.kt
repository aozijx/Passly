package com.aozijx.passly.ui.features.vault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.ui.features.backup.BackupCoordinator
import com.aozijx.passly.ui.features.main.MainViewModel
import com.aozijx.passly.ui.features.vault.components.VaultContentTopBar
import com.aozijx.passly.ui.features.vault.components.VaultDialogs
import com.aozijx.passly.ui.features.vault.components.VaultPagerContent
import com.aozijx.passly.ui.features.vault.components.fab.VaultFab
import com.aozijx.passly.ui.features.vault.internal.rememberVaultActionProvider
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultContent(
    mainViewModel: MainViewModel,
    activity: FragmentActivity,
    vaultViewModel: VaultViewModel,
    backupCoordinator: BackupCoordinator,
    backupDirectoryUri: String?,
    onSettingsClick: () -> Unit = {},
    onPlainExportClick: () -> Unit = {},
    onShowDetail: (VaultEntry) -> Unit = {}
) {
    val uiState by vaultViewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val vaultDisplayViewModel: VaultDisplayViewModel = hiltViewModel()
    val vaultDisplayConfig by vaultDisplayViewModel.config.collectAsStateWithLifecycle()

    val perTypeStyleMap = remember(vaultDisplayConfig.perTypeMap) {
        vaultDisplayConfig.perTypeMap
    }
    var isFabVisible by remember { mutableStateOf(true) }

    val actionProvider = rememberVaultActionProvider(
        activity = activity,
        mainViewModel = mainViewModel,
        vaultViewModel = vaultViewModel,
        backupCoordinator = backupCoordinator,
        backupDirectoryUri = backupDirectoryUri,
        uiState = uiState,
        onShowDetail = onShowDetail,
        isFabVisible = { isFabVisible = it }
    )

    val initialTabIndex = uiState.visibleTabs.indexOf(uiState.selectedTab).coerceAtLeast(0)
    val pagerState =
        rememberPagerState(initialPage = initialTabIndex) { uiState.visibleTabs.size.coerceAtLeast(1) }

    LaunchedEffect(uiState.visibleTabs, uiState.selectedTab) {
        if (uiState.visibleTabs.isEmpty()) return@LaunchedEffect
        if (uiState.selectedTab !in uiState.visibleTabs) {
            vaultViewModel.selectTab(uiState.visibleTabs.first())
            return@LaunchedEffect
        }
        val targetIndex = uiState.visibleTabs.indexOf(uiState.selectedTab)
        if (pagerState.settledPage != targetIndex && pagerState.pageCount > targetIndex) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }

    LaunchedEffect(pagerState, uiState.visibleTabs) {
        snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect { page ->
            val newTab = uiState.visibleTabs.getOrNull(page) ?: return@collect
            if (newTab != uiState.selectedTab) {
                vaultViewModel.selectTab(newTab)
            }
        }
    }

    LaunchedEffect(scrollBehavior.state.collapsedFraction, vaultDisplayConfig.isStatusBarAutoHide) {
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (!vaultDisplayConfig.isStatusBarAutoHide) {
            insetsController.show(WindowInsetsCompat.Type.statusBars())
            return@LaunchedEffect
        }
        if (scrollBehavior.state.collapsedFraction > 0.6f) {
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        } else if (scrollBehavior.state.collapsedFraction < 0.4f) {
            insetsController.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = actionProvider.onUpdateInteraction
            )
            .then(
                if (vaultDisplayConfig.isTopBarCollapsible
                    || vaultDisplayConfig.isTabBarCollapsible
                    || vaultDisplayConfig.isStatusBarAutoHide
                ) {
                    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                } else Modifier
            )
            .nestedScroll(actionProvider.fabScrollConnection),
        topBar = {
            VaultContentTopBar(
                vaultViewModel = vaultViewModel,
                uiState = uiState,
                scrollBehavior = scrollBehavior,
                onExportClick = actionProvider.onExportClick,
                onPlainJsonExportClick = onPlainExportClick,
                onImportClick = actionProvider.onImportClick,
                onSettingsClick = onSettingsClick,
                isStatusBarAutoHide = vaultDisplayConfig.isStatusBarAutoHide,
                isTopBarCollapsible = vaultDisplayConfig.isTopBarCollapsible,
                isTabBarCollapsible = vaultDisplayConfig.isTabBarCollapsible
            )
        },
        floatingActionButton = {
            VaultFab(viewModel = vaultViewModel, isVisible = isFabVisible)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        VaultPagerContent(
            pagerState = pagerState,
            uiState = uiState,
            perTypeStyleMap = perTypeStyleMap,
            swipeLeftAction = vaultDisplayConfig.swipeLeftAction,
            swipeRightAction = vaultDisplayConfig.swipeRightAction,
            isSwipeEnabled = vaultDisplayConfig.isSwipeEnabled,
            onSwipeTriggered = actionProvider.onSwipeTriggered,
            vaultViewModel = vaultViewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }

    VaultDialogs(
        mainViewModel = mainViewModel,
        activity = activity,
        vaultViewModel = vaultViewModel,
        backupCoordinator = backupCoordinator,
        onUpdateInteraction = actionProvider.onUpdateInteraction
    )
}