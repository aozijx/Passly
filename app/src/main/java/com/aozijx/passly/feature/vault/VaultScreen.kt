package com.aozijx.passly.feature.vault

import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.feature.vault.action.rememberVaultActionProvider
import com.aozijx.passly.feature.vault.components.dialog.VaultDialogs
import com.aozijx.passly.feature.vault.components.fab.VaultFab
import com.aozijx.passly.feature.vault.components.list.VaultPagerContent
import com.aozijx.passly.feature.vault.components.topbar.VaultContentTopBar
import com.aozijx.passly.feature.vault.contract.VaultEffect
import com.aozijx.passly.feature.vault.display.VaultDisplayViewModel
import com.aozijx.passly.feature.vault.model.AddType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultContent(
    vaultViewModel: VaultViewModel,
    requestAuthentication: (onSuccess: () -> Unit) -> Unit,
    requestReauthentication: (onSuccess: () -> Unit) -> Unit,
    onUserInteraction: () -> Unit,
    scannerContent: @Composable ((OtpConfig) -> Unit, () -> Unit) -> Unit,
    onAddPassword: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onShowDetail: (EntryListItem) -> Unit = {},
    isDatabaseInitializing: Boolean = false
) {
    val context = LocalContext.current
    val uiState by vaultViewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val vaultDisplayViewModel: VaultDisplayViewModel = hiltViewModel()
    val vaultDisplayConfig by vaultDisplayViewModel.config.collectAsStateWithLifecycle()

    val entryCardPresentations = vaultDisplayConfig.style.entryCardPresentations
    var isFabVisible by remember { mutableStateOf(true) }

    val actionProvider = rememberVaultActionProvider(
        vaultViewModel = vaultViewModel,
        totpStates = vaultViewModel.totpStatesFlow,
        requestAuthentication = requestAuthentication,
        requestReauthentication = requestReauthentication,
        onUserInteraction = onUserInteraction,
        onShowDetail = onShowDetail,
        isFabVisible = { isFabVisible = it }
    )

    val initialTabIndex = uiState.visibleTabs.indexOf(uiState.selectedTab).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialTabIndex) {
        uiState.visibleTabs.size.coerceAtLeast(1)
    }

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
            vaultViewModel.selectTab(newTab)
        }
    }

    val activity = context as? FragmentActivity
    LaunchedEffect(scrollBehavior, vaultDisplayConfig.layout.hideSystemBars, activity) {
        activity ?: return@LaunchedEffect
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (!vaultDisplayConfig.layout.hideSystemBars) {
            insetsController.show(WindowInsetsCompat.Type.statusBars())
            return@LaunchedEffect
        }

        snapshotFlow {
            when {
                scrollBehavior.state.collapsedFraction > 0.6f -> true
                scrollBehavior.state.collapsedFraction < 0.4f -> false
                else -> null
            }
        }.filterNotNull().distinctUntilChanged().collect { shouldHide ->
            if (shouldHide) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    LaunchedEffect(vaultViewModel, context) {
        vaultViewModel.effects.collect { effect ->
            val message = when (effect) {
                is VaultEffect.ShowError -> effect.message
                is VaultEffect.ShowToast -> effect.message
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(activity) {
        onDispose {
            activity?.let {
                WindowCompat.getInsetsController(it.window, it.window.decorView)
                    .show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (vaultDisplayConfig.layout.collapseTopBarOnScroll
                    || vaultDisplayConfig.layout.collapseTabBarOnScroll
                    || vaultDisplayConfig.layout.hideSystemBars
                ) {
                    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                } else Modifier
            )
            .nestedScroll(actionProvider.fabScrollConnection),
        topBar = {
            VaultContentTopBar(
                uiState = uiState,
                selectedTabIndex = pagerState.currentPage,
                maxTabsWithoutScroll = vaultDisplayConfig.layout.tabBarMaxTabsWithoutScroll,
                scrollBehavior = scrollBehavior,
                onSettingsClick = onSettingsClick,
                isStatusBarAutoHide = vaultDisplayConfig.layout.hideSystemBars,
                isTopBarCollapsible = vaultDisplayConfig.layout.collapseTopBarOnScroll,
                isTabBarCollapsible = vaultDisplayConfig.layout.collapseTabBarOnScroll,
                isDatabaseInitializing = isDatabaseInitializing,
                onSearchQueryChange = { vaultViewModel.onSearchQueryChange(it) },
                onToggleSearch = { vaultViewModel.toggleSearch(it) },
                onClearCategory = { vaultViewModel.clearSelectedCategory() },
                onToggleTotpVisibility = { vaultViewModel.toggleShowTOTPCode() },
                onCategorySelected = { vaultViewModel.setSelectedCategory(it) },
                onSortSelected = { vaultViewModel.selectSortOption(it) },
                onSelectTab = { vaultViewModel.selectTab(it) }
            )
        },
        floatingActionButton = {
            VaultFab(
                onAddTypeSelected = { type ->
                    if (type == AddType.PASSWORD) {
                        onAddPassword()
                    } else {
                        vaultViewModel.setAddType(type)
                    }
                },
                isVisible = isFabVisible
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        VaultPagerContent(
            pagerState = pagerState,
            uiState = uiState,
            entryCardPresentations = entryCardPresentations,
            totpStates = vaultViewModel.totpStatesFlow,
            swipeLeftAction = vaultDisplayConfig.interaction.swipeLeftAction,
            swipeRightAction = vaultDisplayConfig.interaction.swipeRightAction,
            isSwipeEnabled = vaultDisplayConfig.interaction.isSwipeEnabled,
            onSwipeTriggered = actionProvider.onSwipeTriggered,
            onItemClick = { onShowDetail(it) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }

    VaultDialogs(
        uiState = uiState,
        vaultViewModel = vaultViewModel,
        requestAuthentication = requestAuthentication,
        onUpdateInteraction = actionProvider.onUpdateInteraction,
        scannerContent = scannerContent
    )
}
