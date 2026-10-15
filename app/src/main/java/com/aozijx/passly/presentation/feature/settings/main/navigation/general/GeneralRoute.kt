package com.aozijx.passly.presentation.feature.settings.main.navigation.general

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.aozijx.passly.presentation.feature.settings.main.general.DiagnosticsSettingsAction
import com.aozijx.passly.presentation.feature.settings.main.general.DiagnosticsSettingsEffect
import com.aozijx.passly.presentation.feature.settings.main.general.DiagnosticsSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.general.GeneralSettingsAction
import com.aozijx.passly.presentation.feature.settings.main.general.GeneralSettingsEffect
import com.aozijx.passly.presentation.feature.settings.main.general.GeneralSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.ui.general.AboutSettingsSection
import com.aozijx.passly.presentation.feature.settings.ui.general.CacheSettingsSection
import com.aozijx.passly.presentation.feature.settings.ui.general.LogSettingsSection
import com.aozijx.passly.presentation.feature.settings.ui.main.SettingsSecondaryPage
import com.aozijx.passly.presentation.feature.settings.ui.main.component.SettingsGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GeneralRoute(
    onBack: (() -> Unit)?,
) {
    val context = LocalContext.current
    val generalViewModel: GeneralSettingsViewModel = hiltViewModel()
    val generalState by generalViewModel.uiState.collectAsStateWithLifecycle()
    val diagnosticsViewModel: DiagnosticsSettingsViewModel = hiltViewModel()
    val diagnosticsState by diagnosticsViewModel.uiState.collectAsStateWithLifecycle()
    val cacheClearedMessage = stringResource(R.string.settings_general_cache_cleared)
    val featurePendingMessage = stringResource(R.string.settings_general_feature_pending)
    val logExportFailedMessage = stringResource(R.string.settings_log_export_failed)
    val logSaveCompletedMessage = stringResource(R.string.settings_log_save_completed)
    val diagnosticsDocumentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            diagnosticsViewModel.onAction(
                DiagnosticsSettingsAction.SaveToDocument(uri.toString()),
            )
        }
    }

    LaunchedEffect(diagnosticsViewModel) {
        diagnosticsViewModel.onAction(DiagnosticsSettingsAction.RefreshLogs)
    }

    LaunchedEffect(generalViewModel, cacheClearedMessage, featurePendingMessage) {
        generalViewModel.effects.collect { effect ->
            when (effect) {
                GeneralSettingsEffect.CacheCleared ->
                    Toast.makeText(context, cacheClearedMessage, Toast.LENGTH_SHORT).show()

                GeneralSettingsEffect.OpenAppDetails -> {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        },
                    )
                }

                GeneralSettingsEffect.OpenTerms,
                GeneralSettingsEffect.OpenPrivacyPolicy,
                GeneralSettingsEffect.OpenOpenSourceLicenses ->
                    Toast.makeText(context, featurePendingMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(diagnosticsViewModel, logExportFailedMessage, logSaveCompletedMessage) {
        diagnosticsViewModel.events.collect { effect ->
            when (effect) {
                DiagnosticsSettingsEffect.SelectSaveDocument ->
                    diagnosticsDocumentPicker.launch(DIAGNOSTICS_FILE_NAME)
                DiagnosticsSettingsEffect.SaveCompleted ->
                    Toast.makeText(context, logSaveCompletedMessage, Toast.LENGTH_SHORT).show()
                DiagnosticsSettingsEffect.ExportFailed ->
                    Toast.makeText(context, logExportFailedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.GENERAL.titleRes),
        onBack = onBack,
    ) {
        item {
            SettingsSection {
                CacheSettingsSection(
                    state = generalState.cache,
                    onClearCache = {
                        generalViewModel.onAction(GeneralSettingsAction.ClearCache)
                    },
                )

                Spacer(Modifier.height(24.dp))

                LogSettingsSection(
                    state = diagnosticsState,
                    onOpenViewer = {
                        diagnosticsViewModel.onAction(DiagnosticsSettingsAction.OpenViewer)
                    },
                    onCloseViewer = {
                        diagnosticsViewModel.onAction(DiagnosticsSettingsAction.CloseViewer)
                    },
                    onSave = {
                        diagnosticsViewModel.onAction(DiagnosticsSettingsAction.RequestSave)
                    },
                    onShare = {
                        diagnosticsViewModel.onAction(DiagnosticsSettingsAction.Share)
                    },
                    onRequestClear = {
                        diagnosticsViewModel.onAction(DiagnosticsSettingsAction.RequestClear)
                    },
                    onConfirmClear = {
                        diagnosticsViewModel.onAction(DiagnosticsSettingsAction.ConfirmClear)
                    },
                    onDismissClear = {
                        diagnosticsViewModel.onAction(DiagnosticsSettingsAction.DismissClear)
                    },
                )

                Spacer(Modifier.height(24.dp))

                AboutSettingsSection(
                    appVersion = BuildConfig.VERSION_NAME,
                    onAppDetailsClick = generalViewModel::openAppDetails,
                    onTermsClick = generalViewModel::openTerms,
                    onPrivacyPolicyClick = generalViewModel::openPrivacyPolicy,
                    onOpenSourceLicensesClick = generalViewModel::openOpenSourceLicenses,
                )
            }
        }
    }
}

private const val DIAGNOSTICS_FILE_NAME = "passly_diagnostics.txt"
