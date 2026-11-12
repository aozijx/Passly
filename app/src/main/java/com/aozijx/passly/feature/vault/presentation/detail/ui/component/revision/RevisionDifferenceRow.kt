package com.aozijx.passly.feature.vault.presentation.detail.ui.component.revision

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionDifferenceContentUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionDifferenceUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionRevealActionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionValueUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.resolveText
import java.text.DateFormat
import java.util.Date

@Composable
internal fun RevisionDifferenceRow(
    difference: DetailRevisionDifferenceUiModel,
    onAction: (DetailUiAction) -> Unit,
    revealEnabled: Boolean = true,
) {
    ListItem(
        overlineContent = { Text(stringResource(difference.changeLabelRes)) },
        supportingContent = {
            when (val content = difference.content) {
                is DetailRevisionDifferenceContentUiState.Values -> {
                    Column {
                        Text(stringResource(R.string.revision_before_value, revisionValue(content.before)))
                        Text(stringResource(R.string.revision_after_value, revisionValue(content.after)))
                    }
                }

                DetailRevisionDifferenceContentUiState.ProtectedHidden -> {
                    Text(stringResource(R.string.revision_protected_value_hidden))
                }

                is DetailRevisionDifferenceContentUiState.ProtectedRevealed -> {
                    Text(content.value.useChars(::String))
                }
            }
        },
        trailingContent =
            when (val revealAction = difference.revealAction) {
                DetailRevisionRevealActionUiState.Unavailable -> {
                    null
                }

                is DetailRevisionRevealActionUiState.Available -> {
                    @Composable {
                        TextButton(
                            enabled = revealEnabled,
                            onClick = { onAction(DetailUiAction.RevealRevisionField(revealAction.fieldKey)) },
                        ) {
                            Text(stringResource(R.string.revision_reveal_action))
                        }
                    }
                }
            },
        colors = ListItemDefaults.colors(),
        elevation = ListItemDefaults.elevation(),
    ) {
        Text(difference.label.resolveText())
    }
}

@Composable
private fun revisionValue(value: DetailRevisionValueUiModel): String =
    when (value) {
        DetailRevisionValueUiModel.Absent -> {
            stringResource(R.string.revision_value_absent)
        }

        is DetailRevisionValueUiModel.Text -> {
            value.value
        }

        is DetailRevisionValueUiModel.Flag -> {
            stringResource(if (value.value) R.string.revision_value_yes else R.string.revision_value_no)
        }

        is DetailRevisionValueUiModel.EpochMillis -> {
            val locale = LocalConfiguration.current.locales[0]
            val formatter = remember(locale) { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale) }
            formatter.format(Date(value.value))
        }

        DetailRevisionValueUiModel.Icon.DEFAULT -> {
            stringResource(R.string.revision_icon_default)
        }

        DetailRevisionValueUiModel.Icon.CUSTOM -> {
            stringResource(R.string.revision_icon_custom)
        }

        is DetailRevisionValueUiModel.Links -> {
            pluralStringResource(R.plurals.revision_link_count, value.count, value.count)
        }

        is DetailRevisionValueUiModel.Attachments -> {
            pluralStringResource(R.plurals.revision_attachment_count, value.count, value.count)
        }
    }
