package com.aozijx.passly.feature.detail.internal

import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.model.secret.CardSecret
import com.aozijx.passly.domain.entry.model.secret.LoginSecret

internal fun EntryAggregate.withDetailUsername(username: String): EntryAggregate =
    copy(summary = summary.copy(username = username))

internal fun EntryAggregate.withLoginPassword(password: String): EntryAggregate =
    copy(
        secret = secret.copy(
            login = secret.login?.copy(password = password) ?: LoginSecret(password = password)
        )
    )

internal fun EntryAggregate.withWifiPassword(password: String): EntryAggregate =
    copy(secret = secret.copy(wifi = secret.wifi?.copy(password = password)))

internal fun EntryAggregate.withSshPassphrase(passphrase: String): EntryAggregate =
    copy(secret = secret.copy(ssh = secret.ssh?.copy(passphrase = passphrase)))

internal fun EntryAggregate.withCardNumber(cardNumber: String): EntryAggregate =
    copy(
        secret = secret.copy(
            card = (secret.card ?: CardSecret()).copy(
                cardNumber = cardNumber,
                hasCardNumber = cardNumber.isNotBlank()
            )
        )
    )

internal fun EntryAggregate.withCardCvv(cvv: String): EntryAggregate =
    copy(
        secret = secret.copy(
            card = (secret.card ?: CardSecret()).copy(
                cardCvv = cvv,
                hasCardCvv = cvv.isNotBlank()
            )
        )
    )
