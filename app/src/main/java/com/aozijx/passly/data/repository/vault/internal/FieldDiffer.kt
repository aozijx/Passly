package com.aozijx.passly.data.repository.vault.internal

import com.aozijx.passly.domain.model.VaultEntry

internal fun diffFields(
    old: VaultEntry,
    new: VaultEntry
): List<Triple<String, String?, String?>> {
    val diffs = mutableListOf<Triple<String, String?, String?>>()
    fun check(name: String, oldVal: String?, newVal: String?) {
        if (oldVal != newVal) diffs.add(Triple(name, oldVal, newVal))
    }
    check("title", old.title, new.title)
    check("username", old.username, new.username)
    check("password", old.password, new.password)
    check("email", old.email, new.email)
    check("totpSecret", old.totpSecret, new.totpSecret)
    check("notes", old.notes, new.notes)
    check("cardCvv", old.cardCvv, new.cardCvv)
    check("cardExpiration", old.cardExpiration, new.cardExpiration)
    check("sshPrivateKey", old.sshPrivateKey, new.sshPrivateKey)
    check("cryptoSeedPhrase", old.cryptoSeedPhrase, new.cryptoSeedPhrase)
    check("idNumber", old.idNumber, new.idNumber)
    check("paymentPin", old.paymentPin, new.paymentPin)
    check("securityAnswer", old.securityAnswer, new.securityAnswer)
    return diffs
}