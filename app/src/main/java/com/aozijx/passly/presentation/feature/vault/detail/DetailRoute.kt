package com.aozijx.passly.presentation.feature.vault.detail

import android.widget.Toast
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.presentation.feature.vault.list.action.CopyFieldLabelProvider
import com.aozijx.passly.presentation.ui.shared.media.ImageType
import com.aozijx.passly.presentation.ui.shared.media.rememberImagePicker
import com.aozijx.passly.presentation.feature.vault.detail.ui.DetailContent
import com.aozijx.passly.presentation.feature.vault.detail.ui.DetailEditorOverlays
import com.aozijx.passly.presentation.feature.vault.detail.ui.DetailScreen
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailContentEvent
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailEditorOverlayEvent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DetailRoute(
    entryId: String,
    onBack: () -> Unit,
    onOpenRelatedEntry: (String) -> Unit,
    launchMode: DetailLaunchMode = DetailLaunchMode.VIEW,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val otpUiState by viewModel.otpState.collectAsStateWithLifecycle()
    val copiedMessageFormat = stringResource(R.string.field_copy_success_message)
    val otpLabel = stringResource(R.string.vault_detail_totp_label)
    val usernameLabel = stringResource(R.string.field_username)
    val passwordLabel = stringResource(R.string.password_label)
    var otpQrUri by remember(entryId) { mutableStateOf<String?>(null) }
    val pickFaviconImage = rememberImagePicker { uri, _ ->
        viewModel.onAction(DetailUiAction.PickedFaviconImage(uri))
    }

    LaunchedEffect(entryId, viewModel) {
        viewModel.load(entryId)
    }
    LaunchedEffect(viewModel, context, copiedMessageFormat, otpLabel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is DetailEffect.ShowOtpQr -> otpQrUri = effect.uri
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

    val entry = uiState.entry ?: return
    val presentation = remember(uiState, otpUiState, usernameLabel, passwordLabel) {
        toDetailPresentationModel(uiState, otpUiState, usernameLabel, passwordLabel)
    } ?: return

    LaunchedEffect(entry.id, launchMode) {
        if (launchMode == DetailLaunchMode.VIEW) return@LaunchedEffect
        if (entry.username.isNotEmpty()) {
            viewModel.onAction(
                DetailUiAction.StartFieldEdit(RevealedFieldKey.USERNAME, entry.username),
            )
        } else if (SensitiveFieldKey.PASSWORD in uiState.sensitiveFieldKeys) {
            viewModel.onAction(
                DetailUiAction.StartFieldEdit(RevealedFieldKey.PASSWORD, ""),
            )
        }
    }

    DetailScreen(
        model = presentation.header,
        onBack = onBack,
        onTitleChanged = { viewModel.onAction(DetailUiAction.UpdateEditedTitle(it)) },
        onTitleEditStarted = { viewModel.onAction(DetailUiAction.StartTitleEdit) },
        onTitleSaved = { viewModel.onAction(DetailUiAction.SaveTitle) },
        onFavoriteToggled = { viewModel.onAction(DetailUiAction.ToggleFavorite) },
    ) { modifier ->
        DetailContent(
            model = presentation.content,
            otpQrUri = otpQrUri,
            onEvent = { event ->
                if (event is DetailContentEvent.OpenRelatedEntry) {
                    onOpenRelatedEntry(event.id)
                } else {
                    event.toDetailUiAction()?.let(viewModel::onAction)
                }
            },
            onOtpQrDismiss = { otpQrUri = null },
            modifier = modifier,
        )
    }

    DetailEditorOverlays(
        model = presentation.overlays,
        onEvent = { event ->
            if (event == DetailEditorOverlayEvent.UploadFavicon) {
                pickFaviconImage(ImageType.SCREEN)
            } else {
                event.toDetailUiAction()?.let(viewModel::onAction)
            }
        },
    )
}
