package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential

/** Applies one supported logical field; unrelated and unrevealed credential fields are preserved. */
object EntryFieldEditor {
    fun edit(entry: Entry, field: FieldKey, value: String): EntryEditResult {
        if (!EntryTypeDefinitions[entry.type].supports(field)) return EntryEditResult.Unsupported
        val updated = when (field) {
            FieldKey.USERNAME -> entry.copy(profile = entry.profile.copy(username = value))
            FieldKey.PASSWORD -> {
                val credential = when (val current = entry.secret.credential) {
                    is WifiCredential -> current.copy(password = value)
                    is LoginCredential -> current.copy(password = value)
                    else -> return EntryEditResult.Unsupported
                }
                entry.copy(secret = entry.secret.copy(credential = credential))
            }
            FieldKey.CARD_HOLDER, FieldKey.CARD_NUMBER, FieldKey.CARD_CVV,
            FieldKey.CARD_ISSUING_BANK, FieldKey.CARD_BANK_BRANCH, FieldKey.CARD_PRODUCT,
            FieldKey.CARD_CURRENCY, FieldKey.CARD_BANK_PHONE, FieldKey.BILLING_ADDRESS -> {
                val card = entry.secret.card ?: return EntryEditResult.Unsupported
                val text = value.trim().takeIf(String::isNotEmpty)
                val credential = when (field) {
                    FieldKey.CARD_HOLDER -> card.copy(cardHolder = text)
                    FieldKey.CARD_NUMBER -> card.copy(cardNumber = value)
                    FieldKey.CARD_CVV -> card.copy(cardCvv = value)
                    FieldKey.CARD_ISSUING_BANK -> card.copy(issuingBank = text)
                    FieldKey.CARD_BANK_BRANCH -> card.copy(bankBranch = text)
                    FieldKey.CARD_PRODUCT -> card.copy(cardProduct = text)
                    FieldKey.CARD_CURRENCY -> card.copy(currency = text)
                    FieldKey.CARD_BANK_PHONE -> card.copy(bankPhone = text)
                    FieldKey.BILLING_ADDRESS -> card.copy(billingAddress = text)
                }
                entry.copy(secret = entry.secret.copy(credential = credential))
            }
            FieldKey.SSH_PASSPHRASE -> {
                val ssh = entry.secret.ssh ?: return EntryEditResult.Unsupported
                entry.copy(secret = entry.secret.copy(credential = ssh.copy(passphrase = value)))
            }
            else -> return EntryEditResult.Unsupported
        }
        val violations = EntryValidation.validateEditedField(updated, field)
        return if (violations.isEmpty()) EntryEditResult.Updated(updated) else EntryEditResult.Invalid(violations)
    }
}
