package com.aozijx.passly.presentation.feature.settings.main.general

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.presentation.ui.settings.general.AboutSettingsSection
import com.aozijx.passly.presentation.ui.settings.general.CacheSettingsSection
import com.aozijx.passly.presentation.ui.settings.general.LogSettingsSection

@Composable
internal fun GeneralDetail() {
    val context = LocalContext.current
    val generalViewModel: GeneralSettingsViewModel = hiltViewModel()
    val generalState by generalViewModel.uiState.collectAsStateWithLifecycle()
    val diagnosticsViewModel: DiagnosticsSettingsViewModel = hiltViewModel()
    val diagnosticsState by diagnosticsViewModel.uiState.collectAsStateWithLifecycle()
    val cacheClearedMessage = stringResource(R.string.settings_general_cache_cleared)
    val featurePendingMessage = stringResource(R.string.settings_general_feature_pending)
    val logExportFailedMessage = stringResource(R.string.settings_log_export_failed)

    // 一次性效果（MVI）：缓存清理提示与页面级跳转均由页面 VM 发出，UI 只负责执行。
    LaunchedEffect(generalViewModel, cacheClearedMessage, featurePendingMessage) {
        generalViewModel.effects.collect { effect ->
            when (effect) {
                GeneralSettingsEffect.CacheCleared ->
                    Toast.makeText(context, cacheClearedMessage, Toast.LENGTH_SHORT).show()

                GeneralSettingsEffect.OpenAppDetails -> {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }

                GeneralSettingsEffect.OpenTerms,
                GeneralSettingsEffect.OpenPrivacyPolicy,
                GeneralSettingsEffect.OpenOpenSourceLicenses ->
                    Toast.makeText(context, featurePendingMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(diagnosticsViewModel, logExportFailedMessage) {
        diagnosticsViewModel.events.collect { effect ->
            when (effect) {
                DiagnosticsSettingsEffect.ExportFailed ->
                    Toast.makeText(context, logExportFailedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))

        CacheSettingsSection(
            cacheSize = generalState.cacheSize,
            isLoading = generalState.isCalculating,
            onClearCache = {
                generalViewModel.onAction(GeneralSettingsAction.ClearCache)
            }
        )

        Spacer(Modifier.height(24.dp))

        LogSettingsSection(
            fileLoggingEnabled = diagnosticsState.fileLoggingEnabled,
            isViewerOpen = diagnosticsState.isViewerOpen,
            logContent = diagnosticsState.logContent,
            logByteCount = diagnosticsState.logByteCount,
            isClearConfirmationOpen = diagnosticsState.isClearConfirmationOpen,
            onFileLoggingEnabledChange = {
                diagnosticsViewModel.onAction(DiagnosticsSettingsAction.SetFileLoggingEnabled(it))
            },
            onOpenViewer = { diagnosticsViewModel.onAction(DiagnosticsSettingsAction.OpenViewer) },
            onCloseViewer = { diagnosticsViewModel.onAction(DiagnosticsSettingsAction.CloseViewer) },
            onExport = { diagnosticsViewModel.onAction(DiagnosticsSettingsAction.Export) },
            onRequestClear = { diagnosticsViewModel.onAction(DiagnosticsSettingsAction.RequestClear) },
            onConfirmClear = { diagnosticsViewModel.onAction(DiagnosticsSettingsAction.ConfirmClear) },
            onDismissClear = { diagnosticsViewModel.onAction(DiagnosticsSettingsAction.DismissClear) },
        )

        Spacer(Modifier.height(24.dp))

        AboutSettingsSection(
            appVersion = BuildConfig.VERSION_NAME,
            onAppDetailsClick = { generalViewModel.openAppDetails() },
            onTermsClick = { generalViewModel.openTerms() },
            onPrivacyPolicyClick = { generalViewModel.openPrivacyPolicy() },
            onOpenSourceLicensesClick = { generalViewModel.openOpenSourceLicenses() }
        )
    }
}
