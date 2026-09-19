package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionKey
import com.aozijx.passly.presentation.ui.shared.components.AppPackagePickerItemUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialFieldUiState
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialSectionUiState
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailActivityTypeUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailActivityUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailAssociatedInfoUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailBankCardUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailBodyUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailContentUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailEditorOverlaysUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailEntryTypeUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailHeaderUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailIconCardUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailIdentityUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailMetadataUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailNotesUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailOtpUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailPasskeyUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailPresentationModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailSectionUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailSeedPhraseUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailSshUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailWifiUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.RelatedEntryUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.ScopedSensitiveText

internal fun detailHeaderUiModel(
    entry: Entry,
    state: DetailUiState,
) = DetailHeaderUiModel(
    title = entry.title,
    favorite = entry.favorite,
    editedTitle = state.editedTitle,
    isEditingTitle = state.isEditingTitle,
)

internal fun detailContentUiModel(
    entry: Entry,
    state: DetailUiState,
) = DetailContentUiModel(
    relatedEntries = state.relatedEntries.map {
        RelatedEntryUiModel(it.id.value, it.title, DetailEntryTypeUiModel.valueOf(it.type.name))
    },
    metadata = DetailMetadataUiModel(entry.createdAt, entry.updatedAt),
    activities = state.history.map {
        DetailActivityUiModel(
            type = DetailActivityTypeUiModel.valueOf(it.activityType.name),
            source = it.source,
            createdAt = it.createdAt,
        )
    },
)

internal fun detailOtpUiModel(otp: OtpCodeState?): DetailOtpUiModel? = otp?.let {
    DetailOtpUiModel(it.code, it.progress, it.isLoading, it.error != null)
}

internal fun toDetailPresentationModel(
    state: DetailUiState,
    otp: OtpCodeState?,
    usernameLabel: String,
    passwordLabel: String,
): DetailPresentationModel? {
    val entry = state.entry ?: return null
    val shared = detailContentUiModel(entry, state)
    val revealedUsername = state.revealed(RevealedFieldKey.USERNAME)
    val revealedPassword = state.revealed(RevealedFieldKey.PASSWORD)
    val cardholder = state.revealed(RevealedFieldKey.CARDHOLDER).copyToString()
    val cardNumber = state.revealed(RevealedFieldKey.CARD_NUMBER).copyToString()
    val cvv = state.revealed(RevealedFieldKey.CVV).copyToString()
    val paymentPin = state.revealed(RevealedFieldKey.PAYMENT_PIN).copyToString()
    val card = entry.secret.card
    val hasNumber = !card?.cardNumber.isNullOrBlank() || cardNumber != null
    val hasCvv = !card?.cardCvv.isNullOrBlank() || cvv != null || state.fieldEdits.isEditing(RevealedFieldKey.CVV)
    val hasPin = !card?.paymentPin.isNullOrBlank() || paymentPin != null
    val passphrase = state.revealed(RevealedFieldKey.SSH_PASSPHRASE).copyToString()
    val privateKey = state.revealed(RevealedFieldKey.SSH_PRIVATE_KEY).copyToString()

    return DetailPresentationModel(
        header = detailHeaderUiModel(entry, state),
        content = DetailBodyUiModel(
            icon = DetailIconCardUiModel(
                iconName = entry.icon.name,
                iconCustomPath = entry.icon.customReference,
                iconColor = entry.icon.color,
                associatedAppPackage = entry.associations.applicationIds.firstOrNull(),
                entryTypeKey = entry.type.name,
                title = entry.title,
                username = entry.username,
                associatedDomain = entry.associatedDomain,
            ),
            sections = state.sections.mapTo(linkedSetOf()) { DetailSectionUiModel.valueOf(it.name) },
            credential = if (DetailSectionKey.CREDENTIAL in state.sections) {
                CredentialSectionUiState(
                    username = CredentialFieldUiState(
                        visible = entry.username.isNotBlank() || SensitiveFieldKey.PASSWORD !in state.sensitiveFieldKeys,
                        label = usernameLabel,
                        revealedValue = revealedUsername?.asScopedSensitiveText(),
                        isEditing = state.fieldEdits.isEditing(RevealedFieldKey.USERNAME),
                        editedValue = state.fieldEdits.draft(RevealedFieldKey.USERNAME),
                        valueForEditing = revealedUsername.copyToString() ?: entry.username,
                    ),
                    password = CredentialFieldUiState(
                        visible = SensitiveFieldKey.PASSWORD in state.sensitiveFieldKeys || entry.type != EntryType.LOGIN,
                        label = passwordLabel,
                        revealedValue = revealedPassword?.asScopedSensitiveText(),
                        isEditing = state.fieldEdits.isEditing(RevealedFieldKey.PASSWORD),
                        editedValue = state.fieldEdits.draft(RevealedFieldKey.PASSWORD),
                        valueForEditing = revealedPassword.copyToString().orEmpty(),
                    ),
                )
            } else null,
            otp = detailOtpUiModel(otp),
            bankCard = if (DetailSectionKey.BANK_CARD in state.sections) DetailBankCardUiModel(
                cardholder ?: entry.username, cardholder != null,
                cardNumber, cardNumber != null, hasNumber,
                cvv, cvv != null, hasCvv, card?.cardExpiry,
                paymentPin, paymentPin != null, hasPin,
                state.fieldEdits.isEditing(RevealedFieldKey.CARDHOLDER),
                state.fieldEdits.draft(RevealedFieldKey.CARDHOLDER),
                state.fieldEdits.isEditing(RevealedFieldKey.CARD_NUMBER),
                state.fieldEdits.draft(RevealedFieldKey.CARD_NUMBER),
                state.fieldEdits.isEditing(RevealedFieldKey.CVV),
                state.fieldEdits.draft(RevealedFieldKey.CVV),
                (hasNumber && cardNumber == null) || (hasCvv && cvv == null) || (hasPin && paymentPin == null),
            ) else null,
            identity = if (DetailSectionKey.IDENTITY in state.sections) DetailIdentityUiModel(
                SensitiveFieldKey.IDENTITY_NUMBER in state.sensitiveFieldKeys,
                state.revealed(RevealedFieldKey.ID_NUMBER).copyToString(),
                state.revealed(RevealedFieldKey.ID_NUMBER) != null,
                entry.username,
            ) else null,
            wifi = if (DetailSectionKey.WIFI in state.sections) DetailWifiUiModel(
                entry.username,
                revealedPassword.copyToString(),
                revealedPassword != null,
                state.fieldEdits.isEditing(RevealedFieldKey.PASSWORD),
                state.fieldEdits.draft(RevealedFieldKey.PASSWORD),
                entry.secret.wifi?.securityType ?: "WPA",
                entry.secret.wifi?.isHidden ?: false,
            ) else null,
            ssh = if (DetailSectionKey.SSH in state.sections) DetailSshUiModel(
                entry.username,
                SensitiveFieldKey.SSH_PASSPHRASE in state.sensitiveFieldKeys,
                SensitiveFieldKey.SSH_PRIVATE_KEY in state.sensitiveFieldKeys,
                passphrase,
                passphrase != null,
                privateKey,
                privateKey != null,
                state.fieldEdits.isEditing(RevealedFieldKey.SSH_PASSPHRASE),
                state.fieldEdits.draft(RevealedFieldKey.SSH_PASSPHRASE),
                (SensitiveFieldKey.SSH_PRIVATE_KEY in state.sensitiveFieldKeys && privateKey == null) ||
                    (SensitiveFieldKey.SSH_PASSPHRASE in state.sensitiveFieldKeys && passphrase == null),
            ) else null,
            seedPhrase = if (DetailSectionKey.SEED_PHRASE in state.sections) DetailSeedPhraseUiModel(
                present = SensitiveFieldKey.SEED_PHRASE in state.sensitiveFieldKeys,
                revealedValue = state.revealed(RevealedFieldKey.SEED_PHRASE).copyToString(),
            ) else null,
            passkey = if (DetailSectionKey.PASSKEY in state.sections) DetailPasskeyUiModel(
                present = SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE in state.sensitiveFieldKeys,
                revealedValue = state.revealed(RevealedFieldKey.PASSKEY_DATA).copyToString(),
                hardwareKeyInfo = entry.secret.passkey?.hardwareKeyInfo,
            ) else null,
            relatedEntries = shared.relatedEntries,
            tags = entry.tags,
            associations = DetailAssociatedInfoUiModel(
                domain = entry.associatedDomain,
                editedDomain = state.fieldEdits.draft(DetailEditKey.DOMAIN),
                isEditingDomain = state.fieldEdits.isEditing(DetailEditKey.DOMAIN),
            ),
            associatedApps = state.associatedApps.map(DetailInstalledApp::toPackagePickerItemUiModel),
            packagePickerApps = state.packagePickerApps.map(DetailInstalledApp::toPackagePickerItemUiModel),
            notes = DetailNotesUiModel(
                notes = entry.secret.notes,
                editedNotes = state.fieldEdits.draft(DetailEditKey.NOTES),
                isEditing = state.fieldEdits.isEditing(DetailEditKey.NOTES),
            ),
            metadata = shared.metadata,
            activities = shared.activities,
        ),
        overlays = DetailEditorOverlaysUiModel(
            tagEditor = state.tagEditor,
            faviconEditor = state.faviconEditor,
            savingTags = state.savingEdit == DetailEditCompletion.Tags,
            savingIcon = state.savingEdit == DetailEditCompletion.Icon,
        ),
    )
}

internal fun DetailInstalledApp.toPackagePickerItemUiModel() = AppPackagePickerItemUiModel(
    label = label,
    packageName = packageName,
)

internal fun SensitiveValue?.asScopedSensitiveText(): ScopedSensitiveText {
    val source = this ?: return ScopedSensitiveText.Empty
    return object : ScopedSensitiveText {
        override val isEmpty: Boolean get() = source.isEmpty

        override fun <R> useChars(block: (CharArray) -> R): R {
            val chars = source.toCharArray()
            return try {
                block(chars)
            } finally {
                chars.fill('\u0000')
            }
        }

        override fun toString() = "***"
    }
}

private fun SensitiveValue?.copyToString(): String? {
    val source = this ?: return null
    val chars = source.toCharArray()
    return try {
        String(chars)
    } finally {
        chars.fill('\u0000')
    }
}
