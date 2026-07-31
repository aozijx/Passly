package com.aozijx.passly.feature.detail.internal

import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.secret.CardSecret
import com.aozijx.passly.domain.entry.model.secret.LoginSecret

internal fun VaultEntry.withDetailUsername(username: String): VaultEntry =
    copy(summary = summary.copy(username = username))

internal fun VaultEntry.withLoginPassword(password: String): VaultEntry =
    copy(
        secret = secret.copy(
            login = secret.login?.copy(password = password) ?: LoginSecret(password = password)
        )
    )

internal fun VaultEntry.withWifiPassword(password: String): VaultEntry =
    copy(secret = secret.copy(wifi = secret.wifi?.copy(password = password)))

internal fun VaultEntry.withSshPassphrase(passphrase: String): VaultEntry =
    copy(secret = secret.copy(ssh = secret.ssh?.copy(passphrase = passphrase)))

internal fun VaultEntry.withCardNumber(cardNumber: String): VaultEntry =
    copy(
        secret = secret.copy(
            card = (secret.card ?: CardSecret()).copy(
                cardNumber = cardNumber,
                hasCardNumber = cardNumber.isNotBlank()
            )
        )
    )

internal fun VaultEntry.withCardCvv(cvv: String): VaultEntry =
    copy(
        secret = secret.copy(
            card = (secret.card ?: CardSecret()).copy(
                cardCvv = cvv,
                hasCardCvv = cvv.isNotBlank()
            )
        )
    )
