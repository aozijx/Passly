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
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.history.RevisionDifferenceKind
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.domain.entry.model.history.RevisionFieldValue
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.feature.vault.presentation.common.labelRes
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionDifferenceUiModel
import java.text.DateFormat
import java.util.Date

@Composable
internal fun RevisionDifferenceRow(
    difference: DetailRevisionDifferenceUiModel,
    onAction: (DetailUiAction) -> Unit,
    revealEnabled: Boolean = true,
) {
    val sensitive = difference.field as? RevisionFieldId.Sensitive
    val protected = sensitive != null || difference.field is RevisionFieldId.ProtectedField ||
        difference.field is RevisionFieldId.ProtectedCustomField
    ListItem(
        overlineContent = {
            Text(stringResource(when (difference.kind) {
                RevisionDifferenceKind.ADDED -> R.string.revision_change_added
                RevisionDifferenceKind.REMOVED -> R.string.revision_change_removed
                RevisionDifferenceKind.CHANGED -> R.string.revision_change_changed
                RevisionDifferenceKind.UNCHANGED -> R.string.revision_change_unchanged
            }))
        },
        supportingContent = {
            if (protected) {
                val revealed = difference.revealedValue?.useChars(::String)
                Text(revealed ?: stringResource(R.string.revision_protected_value_hidden))
            } else {
                Column {
                    Text(stringResource(R.string.revision_before_value, revisionValue(difference.before)))
                    Text(stringResource(R.string.revision_after_value, revisionValue(difference.after)))
                }
            }
        },
        trailingContent = sensitive?.takeUnless { difference.kind == RevisionDifferenceKind.ADDED }?.let { field ->
            @Composable {
                TextButton(enabled = revealEnabled, onClick = { onAction(DetailUiAction.RevealRevisionField(field.key)) }) {
                    Text(stringResource(R.string.revision_reveal_action))
                }
            }
        },
        colors = ListItemDefaults.colors(),
        elevation = ListItemDefaults.elevation(),
    ) { Text(revisionFieldLabel(difference.field)) }
}

@Composable
private fun revisionFieldLabel(field: RevisionFieldId): String = when (field) {
    RevisionFieldId.Title -> stringResource(FieldKey.TITLE.labelRes())
    RevisionFieldId.Username -> stringResource(FieldKey.USERNAME.labelRes())
    RevisionFieldId.PrimaryUrl -> stringResource(FieldKey.PRIMARY_URL.labelRes())
    RevisionFieldId.Domains -> stringResource(FieldKey.DOMAINS.labelRes())
    RevisionFieldId.ApplicationIds -> stringResource(FieldKey.APPLICATION_IDS.labelRes())
    RevisionFieldId.Icon -> stringResource(R.string.revision_field_icon)
    RevisionFieldId.Favorite -> stringResource(FieldKey.FAVORITE.labelRes())
    RevisionFieldId.Tags -> stringResource(FieldKey.TAGS.labelRes())
    RevisionFieldId.ExpiresAt -> stringResource(FieldKey.EXPIRES_AT.labelRes())
    RevisionFieldId.Notes -> stringResource(FieldKey.NOTES.labelRes())
    RevisionFieldId.Attachments -> stringResource(R.string.revision_field_attachments)
    RevisionFieldId.Links -> stringResource(R.string.revision_field_links)
    is RevisionFieldId.CredentialField -> stringResource(field.key.labelRes())
    is RevisionFieldId.ProtectedField -> stringResource(field.key.labelRes())
    is RevisionFieldId.CustomField -> stringResource(R.string.revision_custom_field_label, field.name)
    is RevisionFieldId.ProtectedCustomField -> stringResource(R.string.revision_custom_field_label, field.name)
    is RevisionFieldId.Sensitive -> stringResource(when (field.key) {
        SensitiveFieldKey.PASSWORD -> FieldKey.PASSWORD
        SensitiveFieldKey.CARD_NUMBER -> FieldKey.CARD_NUMBER
        SensitiveFieldKey.CARD_CVV -> FieldKey.CARD_CVV
        SensitiveFieldKey.CARD_PAYMENT_PIN -> FieldKey.PAYMENT_PIN
        SensitiveFieldKey.IDENTITY_NUMBER -> FieldKey.ID_NUMBER
        SensitiveFieldKey.SEED_PHRASE -> FieldKey.SEED_PHRASE
        SensitiveFieldKey.RECOVERY_CODES -> FieldKey.RECOVERY_CODES
        SensitiveFieldKey.SSH_PRIVATE_KEY -> FieldKey.SSH_KEY
        SensitiveFieldKey.SSH_PASSPHRASE -> FieldKey.SSH_PASSPHRASE
        SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE -> FieldKey.PASSKEY_DATA
        SensitiveFieldKey.OTP_SECRET -> FieldKey.OTP_SECRET
    }.labelRes())
}

@Composable
private fun revisionValue(value: RevisionFieldValue?): String = when (value) {
    null -> stringResource(R.string.revision_value_absent)
    is RevisionFieldValue.Text -> value.value
    is RevisionFieldValue.TextSet -> value.values.sorted().joinToString()
    is RevisionFieldValue.Flag -> stringResource(if (value.value) R.string.revision_value_yes else R.string.revision_value_no)
    is RevisionFieldValue.EpochMillis -> {
        val locale = LocalConfiguration.current.locales[0]
        val formatter = remember(locale) { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale) }
        formatter.format(Date(value.value))
    }
    is RevisionFieldValue.Icon -> stringResource(
        if (value.value.customReference.isNullOrBlank()) R.string.revision_icon_default else R.string.revision_icon_custom,
    )
    is RevisionFieldValue.Links -> pluralStringResource(R.plurals.revision_link_count, value.values.size, value.values.size)
    is RevisionFieldValue.AttachmentIds -> pluralStringResource(R.plurals.revision_attachment_count, value.values.size, value.values.size)
}
