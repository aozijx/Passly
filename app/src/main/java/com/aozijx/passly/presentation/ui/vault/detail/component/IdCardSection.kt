package com.aozijx.passly.presentation.ui.vault.detail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailIdentityUiModel

@Composable
fun IdCardSection(
    model: DetailIdentityUiModel,
    onIdNumberCopy: () -> Unit,
    onIdNumberReveal: () -> Unit,
    onUsernameCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(label = stringResource(R.string.id_number),
            value = if (model.hasIdNumber) model.idNumber else stringResource(R.string.not_set),
            isRevealed = model.idNumberRevealed || !model.hasIdNumber, onCopy = onIdNumberCopy,
            onEdit = null, onReveal = onIdNumberReveal)
        model.username.takeIf(String::isNotBlank)?.let {
            DetailItem(label = stringResource(R.string.vault_detail_username), value = it, isRevealed = true,
                onCopy = onUsernameCopy, onEdit = null)
        }
    }
}
