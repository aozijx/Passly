package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.autofill.excludeFromAutofill
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerItemUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailAssociatedAppsUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailAssociatedDomainUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailAssociatedDomainValueUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailAssociatedInfoUiModel

@Composable
fun AssociatedInfoSection(
    modifier: Modifier = Modifier,
    model: DetailAssociatedInfoUiModel,
    onDomainEditStarted: () -> Unit,
    onDomainChanged: (String) -> Unit,
    onDomainSaved: (String) -> Unit,
    associatedApps: DetailAssociatedAppsUiState,
    appIcon: @Composable (String) -> ImageBitmap?,
    onPackagePickerRequested: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val isEditingDomain = model.domain is DetailAssociatedDomainUiState.Editing
    var domainInput by remember(isEditingDomain) {
        mutableStateOf(TextFieldValue(model.domain.editDraftOrEmpty()))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AssociatedDomainCard(
            state = model.domain,
            editedValue = domainInput,
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDomainEditStarted()
            },
            onValueChange = {
                domainInput = it
                onDomainChanged(it.text)
            },
            onSave = {
                focusManager.clearFocus()
                onDomainSaved(domainInput.text)
            },
        )

        AssociatedAppsCard(
            apps = associatedApps,
            appIcon = appIcon,
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onPackagePickerRequested()
            },
        )
    }
}

@Composable
private fun AssociatedDomainCard(
    state: DetailAssociatedDomainUiState,
    editedValue: TextFieldValue,
    onLongClick: () -> Unit,
    onValueChange: (TextFieldValue) -> Unit,
    onSave: () -> Unit,
) {
    val isEditing = state is DetailAssociatedDomainUiState.Editing
    InfoGroupCard(title = stringResource(R.string.vault_detail_associated_domain)) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (isEditing) {
                            Modifier
                        } else {
                            Modifier.combinedClickable(onLongClick = onLongClick, onClick = {})
                        },
                    ).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (state) {
                is DetailAssociatedDomainUiState.Editing -> {
                    DomainEditor(
                        value = editedValue,
                        onValueChange = onValueChange,
                        onDone = onSave,
                    )
                }

                is DetailAssociatedDomainUiState.Display -> {
                    val text =
                        when (val value = state.value) {
                            DetailAssociatedDomainValueUiState.NotSet -> stringResource(R.string.not_set)
                            is DetailAssociatedDomainValueUiState.Value -> value.text
                        }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

private fun DetailAssociatedDomainUiState.editDraftOrEmpty(): String =
    when (this) {
        is DetailAssociatedDomainUiState.Display -> ""
        is DetailAssociatedDomainUiState.Editing -> draft
    }

@Composable
private fun AssociatedAppsCard(
    apps: DetailAssociatedAppsUiState,
    appIcon: @Composable (String) -> ImageBitmap?,
    onLongClick: () -> Unit,
) {
    InfoGroupCard(title = stringResource(R.string.vault_detail_associated_package)) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(onLongClick = onLongClick, onClick = {})
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (apps) {
                DetailAssociatedAppsUiState.Empty -> {
                    Text(
                        text = stringResource(R.string.not_set),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }

                is DetailAssociatedAppsUiState.Items -> {
                    apps.apps.forEach { app ->
                        key(app.packageName) {
                            AssociatedAppRow(app, appIcon(app.packageName))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssociatedAppRow(
    app: AppPackagePickerItemUiModel,
    icon: ImageBitmap?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DomainEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onDone: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value.text,
            onValueChange = { onValueChange(value.copy(text = it)) },
            modifier = Modifier.fillMaxWidth().excludeFromAutofill(),
            label = { Text(stringResource(R.string.vault_detail_domain_label)) },
            placeholder = { Text(stringResource(R.string.vault_detail_domain_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDone) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.save))
            }
        }
    }
}
