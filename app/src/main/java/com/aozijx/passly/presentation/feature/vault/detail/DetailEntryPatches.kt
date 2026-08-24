package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential

internal fun Entry.withDetailUsername(username: String): Entry =
    copy(profile = profile.copy(username = username))

internal fun Entry.withLoginPassword(password: String): Entry =
    copy(
        secret = secret.copy(credential = secret.login?.copy(password = password)
            ?: LoginCredential(password = password))
    )

internal fun Entry.withWifiPassword(password: String): Entry =
    copy(secret = secret.copy(credential = requireNotNull(secret.wifi).copy(password = password)))

internal fun Entry.withSshPassphrase(passphrase: String): Entry =
    copy(secret = secret.copy(credential = requireNotNull(secret.ssh).copy(passphrase = passphrase)))

internal fun Entry.withCardNumber(cardNumber: String): Entry =
    copy(
        secret = secret.copy(
            credential = (secret.card ?: CardCredential()).copy(cardNumber = cardNumber)
        )
    )

internal fun Entry.withCardCvv(cvv: String): Entry =
    copy(
        secret = secret.copy(
            credential = (secret.card ?: CardCredential()).copy(cardCvv = cvv)
        )
    )
