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
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.binding.DetailBodyBinding
import com.aozijx.passly.presentation.feature.vault.list.action.CopyFieldLabelProvider
import com.aozijx.passly.presentation.ui.vault.detail.DetailScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DetailRoute(
    entryId: String,
    onBack: () -> Unit,
    onOpenRelatedEntry: (Entry) -> Unit,
    launchMode: DetailLaunchMode = DetailLaunchMode.VIEW,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val otpUiState by viewModel.otpState.collectAsStateWithLifecycle()
    val copiedMessageFormat = stringResource(R.string.field_copy_success_message)
    val otpLabel = stringResource(R.string.vault_detail_totp_label)
    var otpQrUri by remember(entryId) { mutableStateOf<String?>(null) }

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
    val localEditState = remember(entry.id) { DetailLocalEditState(entry) }

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
        model = detailHeaderUiModel(entry, uiState),
        onBack = onBack,
        onTitleChanged = { viewModel.onAction(DetailUiAction.UpdateEditedTitle(it)) },
        onTitleEditStarted = { viewModel.onAction(DetailUiAction.StartTitleEdit) },
        onTitleSaved = { viewModel.onAction(DetailUiAction.SaveTitle) },
        onFavoriteToggled = { viewModel.onAction(DetailUiAction.ToggleFavorite) },
    ) { modifier ->
        DetailBodyBinding(
            modifier = modifier,
            uiState = uiState,
            localEditState = localEditState,
            otpUiState = otpUiState,
            otpQrUri = otpQrUri,
            onAction = viewModel::onAction,
            onOtpQrDismiss = { otpQrUri = null },
            onOpenRelatedEntry = onOpenRelatedEntry,
        )
    }
}
