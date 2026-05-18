package com.aozijx.passly.core.otp

import androidx.core.net.toUri
import com.aozijx.passly.domain.model.core.VaultEntry
import java.net.URLDecoder
import java.net.URLEncoder

data class OtpAuthData(
    val label: String,
    val secret: String,
    val issuer: String?,
    val digits: Int? = null,
    val period: Int? = null,
    val algorithm: String? = null
)

object TotpUtils {

    fun parseOtpAuthUri(uriString: String): OtpAuthData? {
        if (uriString.isBlank() || !uriString.startsWith("otpauth://")) return null
        return try {
            val uri = uriString.toUri()
            if (uri.host != "totp") return null
            val label = URLDecoder.decode(uri.path?.trimStart('/') ?: "", "UTF-8")
            val secret = uri.getQueryParameter("secret") ?: return null
            val issuer = uri.getQueryParameter("issuer")
            val digits = uri.getQueryParameter("digits")?.toIntOrNull()
            val period = uri.getQueryParameter("period")?.toIntOrNull()
            val algorithm = uri.getQueryParameter("algorithm")?.uppercase()
            OtpAuthData(label, secret, issuer, digits, period, algorithm)
        } catch (_: Exception) {
            null
        }
    }

    fun constructOtpAuthUri(entry: VaultEntry, secret: String): String {
        val issuer = URLEncoder.encode(entry.totpIssuer ?: entry.category, "UTF-8")
        val label = URLEncoder.encode(entry.title, "UTF-8")
        val secretEncoded = secret.replace(" ", "").uppercase()

        return "otpauth://totp/$label?secret=$secretEncoded&issuer=$issuer&period=${entry.totpPeriod}&digits=${entry.totpDigits}&algorithm=${entry.totpAlgorithm}"
    }
}