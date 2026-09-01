package com.aozijx.passly.presentation.feature.vault.detail.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.presentation.feature.vault.detail.DetailAuthenticate
import com.aozijx.passly.presentation.feature.vault.detail.DetailEditCompletion
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.EntryEditState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.detailScreenUiModel
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionKey
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionResolver
import com.aozijx.passly.presentation.ui.vault.detail.component.ActivityTimelineSection
import com.aozijx.passly.presentation.ui.vault.detail.component.DetailIconCard
import com.aozijx.passly.presentation.ui.vault.detail.component.DetailScrollableContent
import com.aozijx.passly.presentation.ui.vault.detail.component.MetadataSection
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailIconCardUiModel

@Composable
fun DetailContentHost(
    modifier: Modifier = Modifier,
    uiState: DetailUiState,
    editState: EntryEditState,
    otpUiState: OtpCodeState?,
    otpQrUri: String?,
    onAction: (DetailUiAction) -> Unit,
    onInteraction: () -> Unit,
    onAuthenticate: DetailAuthenticate,
    onCopySensitive: (String) -> Unit,
    onOtpQrDismiss: () -> Unit,
    onOpenRelatedEntry: (Entry) -> Unit,
) {
    val entry = uiState.entry ?: return
    val screenUiModel = detailScreenUiModel(entry, uiState, otpUiState)
    val registeredSections = DetailSectionResolver.resolve(entry)

    LaunchedEffect(uiState.saveCompletionId) {
        if (uiState.saveCompletionId == 0L) return@LaunchedEffect
        when (val completion = uiState.completedEdit) {
            DetailEditCompletion.Notes -> editState.isEditingNotes = false
            DetailEditCompletion.Associations -> {
                editState.isEditingDomain = false
                editState.isEditingPackage = false
            }
            is DetailEditCompletion.SensitiveField -> when (completion.key) {
                RevealedFieldKey.USERNAME,
                RevealedFieldKey.CARDHOLDER,
                -> editState.isEditingUsername = false

                RevealedFieldKey.PASSWORD,
                RevealedFieldKey.CARD_NUMBER,
                RevealedFieldKey.SSH_PASSPHRASE,
                -> editState.isEditingPassword = false

                RevealedFieldKey.CVV -> editState.isEditingTotp = false
                else -> Unit
            }
            else -> Unit
        }
    }

    DetailScrollableContent(modifier = modifier, onInteraction = onInteraction) {
        item {
            DetailIconCard(
                model = DetailIconCardUiModel(
                    iconName = entry.icon.name,
                    iconCustomPath = entry.icon.customReference,
                    iconColor = entry.icon.color,
                    associatedAppPackage = entry.associations.applicationIds.firstOrNull(),
                    entryTypeKey = entry.type.name,
                    title = entry.title,
                    username = entry.username,
                    associatedDomain = entry.associatedDomain,
                ),
                onEdit = { onAction(DetailUiAction.OpenFaviconEditor) },
            )
        }

        if (DetailSectionKey.CREDENTIAL in registeredSections) {
            item { DetailCredentialHost(entry, uiState, editState, onAction, onAuthenticate, onCopySensitive) }
        }
        if (DetailSectionKey.OTP in registeredSections) {
            item { DetailOtpHost(screenUiModel.otp, otpQrUri, onAction, onCopySensitive, onOtpQrDismiss) }
        }
        if (DetailSectionKey.BANK_CARD in registeredSections) {
            item { DetailBankCardHost(entry, uiState, editState, onAction, onAuthenticate, onCopySensitive) }
        }
        if (DetailSectionKey.IDENTITY in registeredSections) {
            item { DetailIdentityHost(entry, uiState, onAction, onAuthenticate, onCopySensitive) }
        }
        if (DetailSectionKey.WIFI in registeredSections) {
            item { DetailWifiHost(entry, uiState, editState, onAction, onAuthenticate, onCopySensitive) }
        }
        if (DetailSectionKey.SSH in registeredSections) {
            item { DetailSshHost(entry, uiState, editState, onAction, onAuthenticate, onCopySensitive) }
        }
        if (DetailSectionKey.SEED_PHRASE in registeredSections) {
            item { DetailSeedPhraseHost(uiState, onAction, onAuthenticate, onCopySensitive) }
        }
        if (DetailSectionKey.PASSKEY in registeredSections) {
            item { DetailPasskeyHost(entry, uiState, onAction, onAuthenticate, onCopySensitive) }
        }
        if (uiState.relatedEntries.isNotEmpty()) {
            item { DetailRelatedEntriesHost(uiState.relatedEntries, screenUiModel.relatedEntries, onOpenRelatedEntry) }
        }
        item { DetailTagsHost(entry, onAction) }
        item { DetailAssociationsHost(entry, editState, onAction) }
        item { DetailNotesHost(entry, editState, onAction) }
        item { MetadataSection(screenUiModel.metadata) }
        item { ActivityTimelineSection(activityList = screenUiModel.activities) }
    }

    DetailEditorOverlaysHost(uiState = uiState, onAction = onAction)
}
