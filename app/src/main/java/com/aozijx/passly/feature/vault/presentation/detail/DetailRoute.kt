package com.aozijx.passly.feature.vault.presentation.detail

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.entry.EntryEditorSeed
import com.aozijx.passly.feature.vault.presentation.common.copyLabelRes
import com.aozijx.passly.feature.vault.presentation.detail.ui.DetailScreen
import com.aozijx.passly.presentation.shared.event.collectPageEffects
import com.aozijx.passly.presentation.shared.media.ImageType
import com.aozijx.passly.presentation.shared.media.rememberImagePicker
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Composable
fun DetailRoute(
    entryId: String,
    onBack: () -> Unit,
    onOpenRelatedEntry: (String) -> Unit,
    onEditFullForm: (EntryEditorSeed.Editable) -> Unit,
    launchMode: DetailLaunchMode = DetailLaunchMode.VIEW,
) {
    val viewModel = hiltViewModel<DetailViewModel>()
    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val editScope = rememberCoroutineScope()
    var editPreparationJob by remember(viewModel) { mutableStateOf<Job?>(null) }
    val presentationState by viewModel.presentation.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val qrState by viewModel.qrState.collectAsStateWithLifecycle()
    val copiedMessageFormat = stringResource(R.string.field_copy_success_message)
    val otpLabel = stringResource(R.string.vault_detail_totp_label)
    val pickFaviconImage =
        rememberImagePicker { uri, _ ->
            viewModel.onAction(DetailUiAction.PickedFaviconImage(uri))
        }

    LaunchedEffect(entryId, viewModel) {
        viewModel.load(entryId, launchMode)
    }
    LaunchedEffect(viewModel, lifecycle, context, resources, copiedMessageFormat, otpLabel) {
        collectPageEffects(lifecycle, viewModel.effects) { effect ->
            val label =
                when (effect) {
                    is DetailEffect.ContentCopied -> resources.getString(effect.fieldKey.copyLabelRes())
                    DetailEffect.OtpCopied -> otpLabel
                }
            Toast.makeText(context, copiedMessageFormat.format(label), Toast.LENGTH_SHORT).show()
        }
    }
    DisposableEffect(viewModel, entryId) {
        onDispose { viewModel.onAction(DetailUiAction.ClearSensitiveState) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        editPreparationJob?.cancel()
        editPreparationJob = null
        viewModel.onAction(DetailUiAction.DismissEntryQr)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshOtpConfiguration()
    }

    val presentation = presentationState
    if (presentation == null) {
        when (uiState.snapshotState) {
            DetailSnapshotState.Loading,
            DetailSnapshotState.Ready,
            DetailSnapshotState.RefreshingAfterRestore,
            -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            DetailSnapshotState.NotFound -> {
                DetailLoadMessage(
                    message = stringResource(R.string.vault_detail_entry_not_found),
                    action = stringResource(R.string.back),
                    onAction = onBack,
                )
            }

            DetailSnapshotState.Failed -> {
                DetailLoadMessage(
                    message = stringResource(R.string.vault_detail_load_failed),
                    action = stringResource(R.string.retry),
                    onAction = { viewModel.onAction(DetailUiAction.RetryDetailLoad) },
                )
            }

            DetailSnapshotState.RefreshFailedAfterRestore -> {
                DetailLoadMessage(
                    message = stringResource(R.string.vault_detail_refresh_failed_after_restore),
                    action = stringResource(R.string.retry),
                    onAction = { viewModel.onAction(DetailUiAction.RetrySnapshotRefresh) },
                )
            }
        }
        return
    }
    DetailScreen(
        model = presentation,
        qrState = qrState,
        onBack = onBack,
        onAction = viewModel::onAction,
        onOpenRelatedEntry = onOpenRelatedEntry,
        onEditFullForm = {
            if (editPreparationJob?.isActive != true) {
                editPreparationJob =
                    editScope.launch {
                        val editable = viewModel.prepareEntryForEdit() ?: return@launch
                        currentCoroutineContext().ensureActive()
                        onEditFullForm(editable)
                    }
            }
        },
        onFaviconUploadRequested = { pickFaviconImage(ImageType.SCREEN) },
    )
}

@Composable
private fun DetailLoadMessage(
    message: String,
    action: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message)
        TextButton(onClick = onAction) { Text(action) }
    }
}
