package com.aozijx.passly.feature.vault.presentation.detail.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.aozijx.passly.feature.vault.presentation.detail.DetailQrState
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.otp.presentation.OtpUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPresentationModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.DetailHistorySheet
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.DetailTopBar
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.EntryQrSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    model: DetailPresentationModel,
    otpState: OtpUiState,
    qrState: DetailQrState,
    onBack: () -> Unit,
    onAction: (DetailUiAction) -> Unit,
    onOpenRelatedEntry: (String) -> Unit,
    onEditFullForm: () -> Unit,
    onFaviconUploadRequested: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailTopBar(
                model = model.header,
                scrollBehavior = scrollBehavior,
                onTitleChanged = { onAction(DetailUiAction.UpdateEditedTitle(it)) },
                onTitleEditStarted = { onAction(DetailUiAction.StartTitleEdit) },
                onTitleSaved = { onAction(DetailUiAction.SaveTitle) },
                onFavoriteToggled = { onAction(DetailUiAction.ToggleFavorite) },
                onEditFullForm = onEditFullForm,
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        DetailBody(
            model = model.content,
            otpState = otpState,
            onAction = onAction,
            onOpenRelatedEntry = onOpenRelatedEntry,
            modifier = Modifier.padding(innerPadding),
        )
    }

    EntryQrSheet(qrState, onDismiss = { onAction(DetailUiAction.DismissEntryQr) })

    DetailEditorOverlays(
        model = model.overlays,
        onAction = onAction,
        onFaviconUploadRequested = onFaviconUploadRequested,
    )
    DetailHistorySheet(
        model = model.overlays.history,
        onAction = onAction,
    )
}
