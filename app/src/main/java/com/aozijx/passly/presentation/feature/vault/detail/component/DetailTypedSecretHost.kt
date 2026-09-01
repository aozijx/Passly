package com.aozijx.passly.presentation.feature.vault.detail.component

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.presentation.feature.vault.detail.DetailAuthenticate
import com.aozijx.passly.presentation.feature.vault.detail.DetailEditCompletion
import com.aozijx.passly.presentation.feature.vault.detail.DetailEntryPatch
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.EntryEditState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.copySensitiveField
import com.aozijx.passly.presentation.ui.vault.detail.component.BankCardSection
import com.aozijx.passly.presentation.ui.vault.detail.component.DetailBankCardFieldUiModel
import com.aozijx.passly.presentation.ui.vault.detail.component.IdCardSection
import com.aozijx.passly.presentation.ui.vault.detail.component.PasskeySection
import com.aozijx.passly.presentation.ui.vault.detail.component.SeedPhraseSection
import com.aozijx.passly.presentation.ui.vault.detail.component.SshKeySection
import com.aozijx.passly.presentation.ui.vault.detail.component.WifiSection
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailBankCardUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailIdentityUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailSshUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailWifiUiModel

/** Maps typed secret feature state and intents to passive section UI. */
@Composable
internal fun DetailBankCardHost(
    entry: Entry,
    uiState: DetailUiState,
    editState: EntryEditState,
    onAction: (DetailUiAction) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onCopySensitive: (String) -> Unit,
) {
    val handler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
    val cardholder = uiState.revealedString(RevealedFieldKey.CARDHOLDER)
    val cardNumber = uiState.revealedString(RevealedFieldKey.CARD_NUMBER)
    val cvv = uiState.revealedString(RevealedFieldKey.CVV)
    val paymentPin = uiState.revealedString(RevealedFieldKey.PAYMENT_PIN)
    val card = entry.secret.card
    val hasNumber = !card?.cardNumber.isNullOrBlank() || cardNumber != null
    val hasCvv = !card?.cardCvv.isNullOrBlank() || cvv != null || editState.isEditingTotp
    val hasPin = !card?.paymentPin.isNullOrBlank() || paymentPin != null

    BankCardSection(
        model = DetailBankCardUiModel(
            cardholder ?: entry.username, cardholder != null,
            cardNumber, cardNumber != null, hasNumber,
            cvv, cvv != null, hasCvv, card?.cardExpiry,
            paymentPin, paymentPin != null, hasPin,
            editState.isEditingUsername, editState.editedUsername,
            editState.isEditingPassword, editState.editedPassword,
            editState.isEditingTotp, editState.editedTotp,
            (hasNumber && cardNumber == null) || (hasCvv && cvv == null) || (hasPin && paymentPin == null),
        ),
        onEditChanged = { field, value ->
            when (field) {
                DetailBankCardFieldUiModel.CARDHOLDER -> editState.editedUsername = value
                DetailBankCardFieldUiModel.CARD_NUMBER -> editState.editedPassword = value
                DetailBankCardFieldUiModel.CVV -> editState.editedTotp = value
                else -> Unit
            }
        },
        onEditStarted = { field, value ->
            when (field) {
                DetailBankCardFieldUiModel.CARDHOLDER -> {
                    editState.editedUsername = value
                    editState.isEditingUsername = true
                }
                DetailBankCardFieldUiModel.CARD_NUMBER -> {
                    editState.editedPassword = value
                    editState.isEditingPassword = true
                }
                DetailBankCardFieldUiModel.CVV -> {
                    editState.editedTotp = value
                    editState.isEditingTotp = true
                }
                else -> Unit
            }
        },
        onEditSaved = { field, value ->
            when (field) {
                DetailBankCardFieldUiModel.CARDHOLDER -> onAction(
                    DetailUiAction.CommitPatch(
                        DetailEntryPatch.Username(value),
                        DetailEditCompletion.SensitiveField(RevealedFieldKey.CARDHOLDER),
                    ),
                )
                DetailBankCardFieldUiModel.CARD_NUMBER -> onAction(
                    DetailUiAction.CommitPatch(
                        DetailEntryPatch.CardNumber(value),
                        DetailEditCompletion.SensitiveField(RevealedFieldKey.CARD_NUMBER),
                    ),
                )
                DetailBankCardFieldUiModel.CVV -> onAction(
                    DetailUiAction.CommitPatch(
                        DetailEntryPatch.CardCvv(value),
                        DetailEditCompletion.SensitiveField(RevealedFieldKey.CVV),
                    ),
                )
                else -> Unit
            }
        },
        onCopy = { field ->
            val (name, revealed, source) = when (field) {
                DetailBankCardFieldUiModel.CARDHOLDER -> Triple(
                    "cardholder", cardholder?.let(OwnedChars::fromString), entry.username,
                )
                DetailBankCardFieldUiModel.CARD_NUMBER -> Triple("card number", cardNumber?.let(OwnedChars::fromString), null)
                DetailBankCardFieldUiModel.CVV -> Triple("CVV", cvv?.let(OwnedChars::fromString), null)
                DetailBankCardFieldUiModel.PAYMENT_PIN -> Triple("payment PIN", paymentPin?.let(OwnedChars::fromString), null)
                DetailBankCardFieldUiModel.EXPIRATION -> {
                    card?.cardExpiry?.let(handler::copy)
                    onAction(DetailUiAction.RecordAction("expiration", ActivityType.COPY_PASSWORD))
                    return@BankCardSection
                }
            }
            copySensitiveField(handler, name, revealed, source)
        },
        onReveal = { field ->
            val key = when (field) {
                DetailBankCardFieldUiModel.CARD_NUMBER -> RevealedFieldKey.CARD_NUMBER
                DetailBankCardFieldUiModel.CVV -> RevealedFieldKey.CVV
                DetailBankCardFieldUiModel.PAYMENT_PIN -> RevealedFieldKey.PAYMENT_PIN
                else -> return@BankCardSection
            }
            if (uiState.revealed(key) != null) onAction(DetailUiAction.RevealField(key, null))
            else onAction(DetailUiAction.RevealHighSensitivityField(key))
        },
        onRevealAll = {
            val keys = buildSet {
                if (hasNumber && cardNumber == null) add(RevealedFieldKey.CARD_NUMBER)
                if (hasCvv && cvv == null) add(RevealedFieldKey.CVV)
                if (hasPin && paymentPin == null) add(RevealedFieldKey.PAYMENT_PIN)
            }
            if (keys.isNotEmpty()) onAction(DetailUiAction.RevealHighSensitivityFields(keys))
            if (cardholder == null) {
                onAction(DetailUiAction.RevealField(RevealedFieldKey.CARDHOLDER, OwnedChars.fromNullableString(entry.username)))
            }
        },
    )
}

@Composable
internal fun DetailIdentityHost(
    entry: Entry,
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onCopySensitive: (String) -> Unit,
) {
    val handler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
    val idNumber = uiState.revealedString(RevealedFieldKey.ID_NUMBER)
    IdCardSection(
        model = DetailIdentityUiModel(
            SensitiveFieldKey.IDENTITY_NUMBER in uiState.sensitiveFieldKeys,
            idNumber,
            idNumber != null,
            entry.username,
        ),
        onIdNumberCopy = { copySensitiveField(handler, "ID number", idNumber?.let(OwnedChars::fromString), null) },
        onIdNumberReveal = {
            if (idNumber != null) onAction(DetailUiAction.RevealField(RevealedFieldKey.ID_NUMBER, null))
            else onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.ID_NUMBER))
        },
        onUsernameCopy = {
            handler.copy(entry.username)
            handler.record("username", ActivityType.COPY_PASSWORD)
        },
    )
}

@Composable
internal fun DetailWifiHost(
    entry: Entry,
    uiState: DetailUiState,
    editState: EntryEditState,
    onAction: (DetailUiAction) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onCopySensitive: (String) -> Unit,
) {
    val handler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
    val password = uiState.revealedString(RevealedFieldKey.PASSWORD)
    WifiSection(
        model = DetailWifiUiModel(
            entry.username, password, password != null,
            editState.isEditingPassword, editState.editedPassword,
            entry.secret.wifi?.securityType ?: "WPA", entry.secret.wifi?.isHidden ?: false,
        ),
        onSsidCopy = {
            handler.copy(entry.username)
            handler.record("SSID", ActivityType.COPY_PASSWORD)
        },
        onPasswordCopy = { copySensitiveField(handler, "wifi password", password?.let(OwnedChars::fromString), entry.secret.wifi?.password) },
        onPasswordReveal = { onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.PASSWORD)) },
        onPasswordEditStarted = {
            editState.editedPassword = password.orEmpty()
            editState.isEditingPassword = true
        },
        onPasswordChanged = { editState.editedPassword = it },
        onPasswordSaved = {
            if (it != password) {
                onAction(
                    DetailUiAction.CommitPatch(
                        DetailEntryPatch.WifiPassword(it),
                        DetailEditCompletion.SensitiveField(RevealedFieldKey.PASSWORD),
                    ),
                )
            }
        },
    )
}

@Composable
internal fun DetailSshHost(
    entry: Entry,
    uiState: DetailUiState,
    editState: EntryEditState,
    onAction: (DetailUiAction) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onCopySensitive: (String) -> Unit,
) {
    val handler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
    val passphrase = uiState.revealedString(RevealedFieldKey.SSH_PASSPHRASE)
    val privateKey = uiState.revealedString(RevealedFieldKey.SSH_PRIVATE_KEY)
    val hasPassphrase = SensitiveFieldKey.SSH_PASSPHRASE in uiState.sensitiveFieldKeys
    val hasPrivateKey = SensitiveFieldKey.SSH_PRIVATE_KEY in uiState.sensitiveFieldKeys
    SshKeySection(
        model = DetailSshUiModel(
            entry.username, passphrase, passphrase != null,
            privateKey, privateKey != null, editState.isEditingPassword,
            editState.editedPassword,
            (hasPrivateKey && privateKey == null) || (hasPassphrase && passphrase == null),
        ),
        onFingerprintCopy = {
            handler.copy(entry.username)
            handler.record("fingerprint", ActivityType.COPY_PASSWORD)
        },
        onPassphraseCopy = { copySensitiveField(handler, "passphrase", passphrase?.let(OwnedChars::fromString), null) },
        onPassphraseReveal = {
            if (passphrase != null) onAction(DetailUiAction.RevealField(RevealedFieldKey.SSH_PASSPHRASE, null))
            else onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.SSH_PASSPHRASE))
        },
        onPassphraseEditStarted = {
            editState.editedPassword = passphrase.orEmpty()
            editState.isEditingPassword = true
        },
        onPassphraseChanged = { editState.editedPassword = it },
        onPassphraseSaved = {
            if (it != passphrase) {
                onAction(
                    DetailUiAction.CommitPatch(
                        DetailEntryPatch.SshPassphrase(it),
                        DetailEditCompletion.SensitiveField(RevealedFieldKey.SSH_PASSPHRASE),
                    ),
                )
            }
        },
        onPrivateKeyClick = {
            if (privateKey == null) onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.SSH_PRIVATE_KEY))
            else copySensitiveField(handler, "private key", OwnedChars.fromString(privateKey), null)
        },
        onRevealAll = {
            val keys = buildSet {
                if (hasPrivateKey && privateKey == null) add(RevealedFieldKey.SSH_PRIVATE_KEY)
                if (hasPassphrase && passphrase == null) add(RevealedFieldKey.SSH_PASSPHRASE)
            }
            if (keys.isNotEmpty()) onAction(DetailUiAction.RevealHighSensitivityFields(keys))
        },
    )
}

@Composable
internal fun DetailSeedPhraseHost(
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onCopySensitive: (String) -> Unit,
) {
    val handler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
    val seed = uiState.revealedString(RevealedFieldKey.SEED_PHRASE)
    SeedPhraseSection(
        hasSeedPhrase = SensitiveFieldKey.SEED_PHRASE in uiState.sensitiveFieldKeys,
        revealedSeedPhrase = seed,
        onCopy = { copySensitiveField(handler, "seed phrase", seed?.let(OwnedChars::fromString), null) },
        onReveal = {
            if (seed != null) onAction(DetailUiAction.RevealField(RevealedFieldKey.SEED_PHRASE, null))
            else onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.SEED_PHRASE))
        },
    )
}

@Composable
internal fun DetailPasskeyHost(
    entry: Entry,
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onCopySensitive: (String) -> Unit,
) {
    val handler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
    val passkeyData = uiState.revealedString(RevealedFieldKey.PASSKEY_DATA)
    val hardwareInfo = entry.secret.passkey?.hardwareKeyInfo
    PasskeySection(
        hasPasskeyData = SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE in uiState.sensitiveFieldKeys,
        revealedPasskeyData = passkeyData,
        hardwareKeyInfo = hardwareInfo,
        onPasskeyCopy = { copySensitiveField(handler, "passkey data", passkeyData?.let(OwnedChars::fromString), null) },
        onPasskeyReveal = {
            if (passkeyData != null) onAction(DetailUiAction.RevealField(RevealedFieldKey.PASSKEY_DATA, null))
            else onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.PASSKEY_DATA))
        },
        onHardwareKeyCopy = {
            hardwareInfo?.let {
                handler.copy(it)
                handler.record("hardware key info", ActivityType.COPY_PASSWORD)
            }
        },
    )
}

private fun DetailUiState.revealedString(key: String): String? =
    revealed(key)?.let { String(it.toCharArray()) }
