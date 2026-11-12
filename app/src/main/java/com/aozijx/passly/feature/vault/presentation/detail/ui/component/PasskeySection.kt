package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHardwareKeyInfoUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPasskeyDataUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPasskeyUiModel

@Composable
fun PasskeySection(
    model: DetailPasskeyUiModel,
    onPasskeyCopy: () -> Unit,
    onPasskeyReveal: () -> Unit,
    onHardwareKeyCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val passkeyDataLabel = stringResource(R.string.passkey_data)
    val hardwareKeyInfoLabel = stringResource(R.string.hardware_key_info)
    val notSet = stringResource(R.string.not_set)
    val passkeyDataState = model.data
    val passkeyData =
        when (passkeyDataState) {
            DetailPasskeyDataUiState.Absent -> notSet
            DetailPasskeyDataUiState.Hidden -> ""
            is DetailPasskeyDataUiState.Revealed -> passkeyDataState.value
        }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailValueCard(
            title = passkeyDataLabel,
            value = passkeyData,
            isRevealed = passkeyDataState !is DetailPasskeyDataUiState.Hidden,
            onCopy = onPasskeyCopy,
            onRevealClick = onPasskeyReveal.takeIf { passkeyDataState !is DetailPasskeyDataUiState.Absent },
        )

        when (val hardwareKeyInfo = model.hardwareKeyInfo) {
            DetailHardwareKeyInfoUiState.NotSet -> {
                Unit
            }

            is DetailHardwareKeyInfoUiState.Available -> {
                DetailValueCard(
                    title = hardwareKeyInfoLabel,
                    value = hardwareKeyInfo.value,
                    isRevealed = true,
                    onCopy = onHardwareKeyCopy,
                )
            }
        }
    }
}
