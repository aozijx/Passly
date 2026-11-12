package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

internal fun DetailUiState.bankCardFieldsToReveal(): Set<DetailField> =
    buildSet {
        if (SensitiveFieldKey.CARD_NUMBER in sensitiveFieldKeys && revealed(FieldKey.CARD_NUMBER) == null) {
            add(DetailField.CARD_NUMBER)
        }
        if (
            (
                SensitiveFieldKey.CARD_CVV in sensitiveFieldKeys ||
                    fieldEdits.edit(DetailDraftKey.EntryField(FieldKey.CARD_CVV)) is DetailFieldEdit.Editing
            ) &&
            revealed(FieldKey.CARD_CVV) == null
        ) {
            add(DetailField.CARD_CVV)
        }
        if (SensitiveFieldKey.CARD_PAYMENT_PIN in sensitiveFieldKeys && revealed(FieldKey.PAYMENT_PIN) == null) {
            add(DetailField.PAYMENT_PIN)
        }
    }

internal fun DetailUiState.sshFieldsToReveal(): Set<DetailField> =
    buildSet {
        if (SensitiveFieldKey.SSH_PRIVATE_KEY in sensitiveFieldKeys && revealed(FieldKey.SSH_KEY) == null) {
            add(DetailField.SSH_PRIVATE_KEY)
        }
        if (SensitiveFieldKey.SSH_PASSPHRASE in sensitiveFieldKeys && revealed(FieldKey.SSH_PASSPHRASE) == null) {
            add(DetailField.SSH_PASSPHRASE)
        }
    }
