package com.aozijx.passly.feature.detail.page

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.feature.detail.DetailAuthenticate
import com.aozijx.passly.feature.detail.components.DetailScrollableContent
import com.aozijx.passly.feature.detail.components.DetailTopBar
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.contract.DetailUiState
import com.aozijx.passly.feature.detail.internal.EntryEditState
import com.aozijx.passly.feature.vault.model.OtpUiState

/**
 * 详情页 UI 组件 (Stateless)
 *
 * 采用状态平铺模式，不直接持有 ViewModel，方便测试和预览。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    initialEntry: VaultEntry,
    uiState: DetailUiState,
    otpUiState: OtpUiState?,
    launchMode: DetailLaunchMode = DetailLaunchMode.VIEW,
    onBack: () -> Unit,
    onEvent: (DetailIntent) -> Unit,
    onUpdateInteraction: () -> Unit,
    onUpdateVaultEntry: (VaultEntry) -> Unit,
    onShowIconPicker: () -> Unit,
    onAutoUnlockTotp: (VaultEntry) -> Unit,
    onAuthenticate: DetailAuthenticate
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // 初始进入和交互更新
    LaunchedEffect(Unit) {
        onUpdateInteraction()
    }

    // 页面数据初始化（同 key 内串联首次 TOTP 自动解锁，避免重复 effect 触发）
    LaunchedEffect(initialEntry.id) {
        onEvent(DetailIntent.Initialize(initialEntry))
        val initialOtpSecret = initialEntry.secret.otp?.config?.secret
        if (!initialOtpSecret.isNullOrBlank()) {
            onAutoUnlockTotp(initialEntry)
        }
    }

    val entry = uiState.entry ?: initialEntry
    val editState = remember(entry) { EntryEditState(entry) }

    // 处理外部启动模式（如编辑 TOTP）
    LaunchedEffect(entry.id, launchMode) {
        if (launchMode == DetailLaunchMode.VIEW) return@LaunchedEffect

        if (entry.username.isNotEmpty()) {
            editState.isEditingUsername = true
        } else if (entry.secret.login?.password?.isNotEmpty() == true) {
            editState.isEditingPassword = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ClipboardUtils.clear(context)
            onEvent(DetailIntent.ClearSensitiveState)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailTopBar(
                entry = entry,
                uiState = uiState,
                scrollBehavior = scrollBehavior,
                onEvent = onEvent,
                onBack = onBack,
                onInteraction = onUpdateInteraction
            )
        }
    ) { innerPadding ->
        DetailScrollableContent(
            modifier = Modifier.padding(innerPadding),
            uiState = uiState,
            editState = editState,
            otpUiState = otpUiState,
            onEvent = onEvent,
            onInteraction = onUpdateInteraction,
            onUpdateVaultEntry = onUpdateVaultEntry,
            onShowIconPicker = onShowIconPicker,
            onAuthenticate = onAuthenticate
        )
    }
}
