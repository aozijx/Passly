package com.aozijx.passly.core.security.otp

import com.aozijx.passly.domain.model.VaultEntry
import java.net.URLEncoder

object TotpUtils {
    fun constructOtpAuthUri(entry: VaultEntry, secret: String): String {
        val type = if (entry.totpAlgorithm.uppercase() == "STEAM") "totp" else "totp"
        val issuer = URLEncoder.encode(entry.category, "UTF-8")
        val label = URLEncoder.encode(entry.title, "UTF-8")
        val secretEncoded = secret.replace(" ", "").uppercase()

        return "otpauth://$type/$label?secret=$secretEncoded&issuer=$issuer&period=${entry.totpPeriod}&digits=${entry.totpDigits}&algorithm=${entry.totpAlgorithm}"
    }
}



