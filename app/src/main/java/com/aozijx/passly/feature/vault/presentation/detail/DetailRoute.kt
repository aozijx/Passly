package com.aozijx.passly.feature.vault.presentation.detail

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.list.action.CopyFieldLabelProvider
import com.aozijx.passly.presentation.shared.media.ImageType
import com.aozijx.passly.presentation.shared.media.rememberImagePicker
import com.aozijx.passly.feature.vault.presentation.detail.ui.DetailScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DetailRoute(
    entryId: String,
    onBack: () -> Unit,
    onOpenRelatedEntry: (String) -> Unit,
    launchMode: DetailLaunchMode = DetailLaunchMode.VIEW,
) {
    val viewModel = hiltViewModel<DetailViewModel>()
    val context = LocalContext.current
    val presentationState by viewModel.presentation.collectAsStateWithLifecycle()
    val qrState by viewModel.qrState.collectAsStateWithLifecycle()
    val copiedMessageFormat = stringResource(R.string.field_copy_success_message)
    val otpLabel = stringResource(R.string.vault_detail_totp_label)
    val pickFaviconImage = rememberImagePicker { uri, _ ->
        viewModel.onAction(DetailUiAction.PickedFaviconImage(uri))
    }

    LaunchedEffect(entryId, viewModel) {
        viewModel.load(entryId, launchMode)
    }
    LaunchedEffect(viewModel, context, copiedMessageFormat, otpLabel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is DetailEffect.ContentCopied -> {
                    val label = effect.fieldKey
                        ?.let(CopyFieldLabelProvider::getCopyLabel)
                        ?: otpLabel
                    Toast.makeText(
                        context,
                        copiedMessageFormat.format(label),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }
    DisposableEffect(viewModel, entryId) {
        onDispose { viewModel.onAction(DetailUiAction.ClearSensitiveState) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.onAction(DetailUiAction.DismissEntryQr)
    }

    val presentation = presentationState ?: return
    DetailScreen(
        model = presentation,
        qrState = qrState,
        onBack = onBack,
        onAction = viewModel::onAction,
        onOpenRelatedEntry = onOpenRelatedEntry,
        onFaviconUploadRequested = { pickFaviconImage(ImageType.SCREEN) },
    )
}
