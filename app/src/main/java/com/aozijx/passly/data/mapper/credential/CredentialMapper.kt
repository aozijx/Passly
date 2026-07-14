package com.aozijx.passly.data.mapper.credential

import com.aozijx.passly.data.model.payload.credential.CredentialPayload
import com.aozijx.passly.domain.model.VaultEntry

fun VaultEntry.toCredentialPayload(): CredentialPayload = CredentialPayload(
    login = toLoginPayload(),
    totp = toTotpPayload(),
    passkey = toPasskeyPayload(),
    card = toCardPayload(),
    identity = toIdentityPayload(),
    wifi = toWifiPayload(),
    securityQuestion = toSecurityQuestionPayload(),
    payment = toPaymentPayload(),
    ssh = toSshPayload(),
    customFields = toCustomFieldsPayload()
)

fun VaultEntry.mergeCredential(payload: CredentialPayload): VaultEntry = this
    .mergeLogin(payload.login)
    .mergeTotp(payload.totp)
    .mergePasskey(payload.passkey)
    .mergeCard(payload.card)
    .mergeIdentity(payload.identity)
    .mergeWifi(payload.wifi)
    .mergeSecurityQuestion(payload.securityQuestion)
    .mergePayment(payload.payment)
    .mergeSsh(payload.ssh)
    .mergeCustomFields(payload.customFields)