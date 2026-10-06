package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType

object WifiCredentialValidation {
    fun validate(
        ssid: String,
        password: String?,
        securityType: WifiSecurityType,
    ): Set<EntryViolation> = buildSet {
        if (ssid.isBlank()) {
            add(EntryViolation(FieldKey.WIFI_SSID, EntryViolationCode.REQUIRED))
        }
        when (securityType) {
            WifiSecurityType.Open -> if (!password.isNullOrEmpty()) {
                add(EntryViolation(FieldKey.PASSWORD, EntryViolationCode.INVALID_FORMAT))
            }

            WifiSecurityType.Wep -> validateWepPassword(password)
            WifiSecurityType.WpaPersonal -> validateWpaPassword(password)
        }
    }

    private fun MutableSet<EntryViolation>.validateWepPassword(password: String?) {
        if (password.isNullOrEmpty()) {
            add(EntryViolation(FieldKey.PASSWORD, EntryViolationCode.REQUIRED))
            return
        }
        val validAsciiLength = password.length == 5 || password.length == 13
        val validHexKey = password.length in setOf(10, 26) && password.all { it.isHexDigit() }
        if (!validAsciiLength && !validHexKey) {
            add(EntryViolation(FieldKey.PASSWORD, EntryViolationCode.INVALID_LENGTH))
        }
    }

    private fun MutableSet<EntryViolation>.validateWpaPassword(password: String?) {
        if (password.isNullOrEmpty()) {
            add(EntryViolation(FieldKey.PASSWORD, EntryViolationCode.REQUIRED))
            return
        }
        val validPassphrase = password.length in 8..63
        val validHexKey = password.length == 64 && password.all { it.isHexDigit() }
        if (!validPassphrase && !validHexKey) {
            add(EntryViolation(FieldKey.PASSWORD, EntryViolationCode.INVALID_LENGTH))
        }
    }

    private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
}
