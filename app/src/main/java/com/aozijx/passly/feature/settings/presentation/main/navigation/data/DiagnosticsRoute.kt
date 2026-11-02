package com.aozijx.passly.feature.settings.presentation.main.navigation.data

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.feature.settings.diagnostics.LogFormat
import com.aozijx.passly.feature.settings.presentation.main.diagnostics.DiagnosticsAction
import com.aozijx.passly.feature.settings.presentation.main.diagnostics.DiagnosticsEffect
import com.aozijx.passly.feature.settings.presentation.main.diagnostics.DiagnosticsViewModel
import com.aozijx.passly.feature.settings.presentation.ui.diagnostics.DiagnosticsScreen
import com.aozijx.passly.presentation.shared.event.collectPageEffects

@Composable
internal fun DiagnosticsRoute(onBack: (() -> Unit)?) {
    val context = LocalContext.current
    val viewModel: DiagnosticsViewModel = hiltViewModel()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val pageOwner = remember(viewModel) { mutableStateOf<Any?>(null) }
    var documentRequestId by remember(viewModel) { mutableStateOf<String?>(null) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val exportFailed = stringResource(R.string.settings_log_export_failed)
    val saveCompleted = stringResource(R.string.settings_log_save_completed)
    val clearFailed = stringResource(R.string.settings_log_clear_failed)

    val jsonlPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(LogFormat.JSONL.mimeType),
        ) { uri ->
            val owner = pageOwner.value
            val requestId = documentRequestId
            documentRequestId = null
            if (owner != null && requestId != null) {
                viewModel.onAction(
                    uri?.let { DiagnosticsAction.SaveToDocument(it.toString(), owner, requestId) }
                        ?: DiagnosticsAction.SaveDocumentCancelled(owner, requestId),
                )
            }
        }
    val tsvPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(LogFormat.TSV.mimeType),
        ) { uri ->
            val owner = pageOwner.value
            val requestId = documentRequestId
            documentRequestId = null
            if (owner != null && requestId != null) {
                viewModel.onAction(
                    uri?.let { DiagnosticsAction.SaveToDocument(it.toString(), owner, requestId) }
                        ?: DiagnosticsAction.SaveDocumentCancelled(owner, requestId),
                )
            }
        }
    val zipPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(LogFormat.ZIP.mimeType),
        ) { uri ->
            val owner = pageOwner.value
            val requestId = documentRequestId
            documentRequestId = null
            if (owner != null && requestId != null) {
                viewModel.onAction(
                    uri?.let { DiagnosticsAction.SaveToDocument(it.toString(), owner, requestId) }
                        ?: DiagnosticsAction.SaveDocumentCancelled(owner, requestId),
                )
            }
        }

    DisposableEffect(viewModel) {
        val owner = viewModel.onPageEntered()
        pageOwner.value = owner
        onDispose {
            pageOwner.value = null
            documentRequestId = null
            viewModel.onPageExited(owner)
        }
    }
    LaunchedEffect(viewModel, lifecycle, context, exportFailed, saveCompleted, clearFailed) {
        collectPageEffects(lifecycle, viewModel.effects) { effect ->
            when (effect) {
                is DiagnosticsEffect.SelectSaveDocument -> {
                    documentRequestId = effect.requestId
                    when (effect.format) {
                        LogFormat.JSONL -> jsonlPicker.launch(effect.fileName)
                        LogFormat.TSV -> tsvPicker.launch(effect.fileName)
                        LogFormat.ZIP -> zipPicker.launch(effect.fileName)
                    }
                }

                DiagnosticsEffect.SaveCompleted -> {
                    Toast.makeText(context, saveCompleted, Toast.LENGTH_SHORT).show()
                }

                DiagnosticsEffect.ExportFailed -> {
                    Toast.makeText(context, exportFailed, Toast.LENGTH_SHORT).show()
                }

                DiagnosticsEffect.ClearFailed -> {
                    Toast.makeText(context, clearFailed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    DiagnosticsScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
    )
}
