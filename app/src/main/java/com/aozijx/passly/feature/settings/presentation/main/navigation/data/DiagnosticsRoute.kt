package com.aozijx.passly.feature.settings.presentation.main.navigation.data

import android.content.ClipData
import android.content.ClipboardManager
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.feature.settings.diagnostics.LogFormat
import com.aozijx.passly.feature.settings.presentation.main.diagnostics.DiagnosticsAction
import com.aozijx.passly.feature.settings.presentation.main.diagnostics.DiagnosticsEffect
import com.aozijx.passly.feature.settings.presentation.main.diagnostics.DiagnosticsViewModel
import com.aozijx.passly.feature.settings.presentation.ui.diagnostics.DiagnosticsScreen

@Composable
internal fun DiagnosticsRoute(onBack: (() -> Unit)?) {
    val context = LocalContext.current
    val viewModel: DiagnosticsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val exportFailed = stringResource(R.string.settings_log_export_failed)
    val saveCompleted = stringResource(R.string.settings_log_save_completed)
    val jsonlPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(LogFormat.JSONL.mimeType),
    ) { uri -> viewModel.onDocumentSelected(uri?.toString()) }
    val tsvPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(LogFormat.TSV.mimeType),
    ) { uri -> viewModel.onDocumentSelected(uri?.toString()) }
    val zipPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(LogFormat.ZIP.mimeType),
    ) { uri -> viewModel.onDocumentSelected(uri?.toString()) }

    LaunchedEffect(viewModel) {
        viewModel.onAction(DiagnosticsAction.PageEntered)
    }
    LaunchedEffect(viewModel, context, exportFailed, saveCompleted) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DiagnosticsEffect.SelectSaveDocument -> when (effect.format) {
                    LogFormat.JSONL -> jsonlPicker.launch(effect.fileName)
                    LogFormat.TSV -> tsvPicker.launch(effect.fileName)
                    LogFormat.ZIP -> zipPicker.launch(effect.fileName)
                }
                is DiagnosticsEffect.CopyText -> context.copySensitiveDiagnostics(effect.text)
                DiagnosticsEffect.SaveCompleted ->
                    Toast.makeText(context, saveCompleted, Toast.LENGTH_SHORT).show()
                DiagnosticsEffect.ExportFailed ->
                    Toast.makeText(context, exportFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    DiagnosticsScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
    )
}

private fun android.content.Context.copySensitiveDiagnostics(text: String) {
    val clip = ClipData.newPlainText("Passly diagnostics", text)
    clip.description.extras = PersistableBundle().apply {
        putBoolean(EXTRA_IS_SENSITIVE, true)
    }
    getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
}

private fun DiagnosticsViewModel.onDocumentSelected(uri: String?) {
    onAction(
        uri?.let { DiagnosticsAction.SaveToDocument(it) } ?: DiagnosticsAction.SaveDocumentCancelled,
    )
}

private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"
