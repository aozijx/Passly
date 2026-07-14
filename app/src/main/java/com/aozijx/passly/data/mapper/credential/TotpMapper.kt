package com.aozijx.passly.data.mapper.credential

import com.aozijx.passly.data.model.payload.credential.TotpPayload
import com.aozijx.passly.domain.model.VaultEntry

fun VaultEntry.toTotpPayload(): TotpPayload? {
    if (totpSecret.isNullOrBlank() && totpIssuer.isNullOrBlank()) {
        return null
    }
    return TotpPayload(
        secret = totpSecret,
        issuer = totpIssuer,
        period = totpPeriod,
        digits = totpDigits,
        algorithm = totpAlgorithm
    )
}

fun VaultEntry.mergeTotp(payload: TotpPayload?): VaultEntry {
    payload ?: return this
    return copy(
        totpSecret = payload.secret,
        totpIssuer = payload.issuer,
        totpPeriod = payload.period,
        totpDigits = payload.digits,
        totpAlgorithm = payload.algorithm
    )
}