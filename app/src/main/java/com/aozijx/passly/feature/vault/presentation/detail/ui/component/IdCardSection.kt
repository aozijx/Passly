package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailIdentityUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.ProtectedDetailFieldUiState

@Composable
fun IdCardSection(
    model: DetailIdentityUiModel,
    onIdNumberCopy: () -> Unit,
    onIdNumberReveal: () -> Unit,
    onUsernameCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val idNumberValue =
            when (val idNumber = model.idNumber) {
                ProtectedDetailFieldUiState.Absent -> stringResource(R.string.not_set)
                ProtectedDetailFieldUiState.Hidden -> ""
                is ProtectedDetailFieldUiState.Revealed -> idNumber.value
            }
        DetailValueCard(
            title = stringResource(R.string.id_number),
            value = idNumberValue,
            isRevealed = model.idNumber !is ProtectedDetailFieldUiState.Hidden,
            onCopy = onIdNumberCopy,
            onRevealClick = onIdNumberReveal,
        )
        DetailValueCard(
            title = stringResource(R.string.vault_detail_username),
            value = model.username,
            isRevealed = true,
            onCopy = onUsernameCopy,
        )
    }
}
