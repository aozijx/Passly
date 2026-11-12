package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailIdentityNumberUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailIdentityUiModel

@Composable
fun IdCardSection(
    model: DetailIdentityUiModel,
    onIdNumberCopy: () -> Unit,
    onIdNumberReveal: () -> Unit,
    onUsernameCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val idNumber = model.idNumber
        val idNumberValue =
            when (idNumber) {
                DetailIdentityNumberUiState.Absent -> stringResource(R.string.not_set)
                DetailIdentityNumberUiState.Hidden -> ""
                is DetailIdentityNumberUiState.Revealed -> idNumber.value
            }
        DetailValueCard(
            title = stringResource(R.string.id_number),
            value = idNumberValue,
            isRevealed = idNumber !is DetailIdentityNumberUiState.Hidden,
            onCopy = onIdNumberCopy,
            onRevealClick = onIdNumberReveal.takeIf { idNumber !is DetailIdentityNumberUiState.Absent },
        )
        DetailValueCard(
            title = stringResource(R.string.vault_detail_username),
            value = model.username,
            isRevealed = true,
            onCopy = onUsernameCopy,
        )
    }
}
