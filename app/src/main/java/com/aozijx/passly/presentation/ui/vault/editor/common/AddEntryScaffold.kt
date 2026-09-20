package com.aozijx.passly.presentation.ui.vault.editor.common

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.shared.components.topbar.PasslyNavigationTopBar

/**
 * 新建条目页面的公共外壳。
 *
 * 新增更多条目类型时只提供各自的表单 slot 和状态；导航、IME、错误提示及保存动作
 * 保持一份实现，避免把不同类型的字段强行塞进一个通用状态模型。
 */
@Composable
fun AddEntryScaffold(
    title: String,
    canSave: Boolean,
    isSaving: Boolean,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    saveActionModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val cleanupAndDo: (() -> Unit) -> Unit = { action ->
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        action()
    }

    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    BackHandler(enabled = !isSaving) {
        cleanupAndDo(onBack)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            PasslyNavigationTopBar(
                title = title,
                onNavigateBack = { cleanupAndDo(onBack) },
                navigationEnabled = !isSaving,
            )
        },
        floatingActionButton = {
            SaveEntryFab(
                canSave = canSave,
                isSaving = isSaving,
                onSave = { cleanupAndDo(onSave) },
                modifier = saveActionModifier,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .padding(bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SaveEntryFab(
    canSave: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = canSave && !isSaving

    val labelVisible = rememberSharedFabLabelVisible(visible = !isSaving)
    SharedAddEntryExtendedFab(
        label = stringResource(R.string.save),
        onClick = { if (enabled) onSave() },
        icon = {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null)
            }
        },
        expanded = !isSaving,
        enabled = enabled,
        labelVisible = labelVisible,
        modifier = modifier
            .then(if (enabled) Modifier else Modifier.semantics { disabled() })
    )
}
