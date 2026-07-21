package com.aozijx.passly.core.util

import android.net.Uri
import androidx.core.net.toUri
import com.aozijx.passly.domain.model.entry.VaultEntry
import java.net.URLDecoder

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
        val otp = entry.credential.twoFactor?.otp
        val label = Uri.encode(entry.title, "UTF-8")
        // 密钥处理：确保无空格且大写
        val secret = secret.replace(" ", "").uppercase()
        // 构建参数映射
        val params = mutableMapOf(
            "secret" to secret,
            "period" to (otp?.period ?: 30).toString(),
            "digits" to (otp?.digits ?: 6).toString(),
            "algorithm" to (otp?.algorithm ?: "SHA1")
        )
        // 优先使用原有的 issuer，没有则用分类
        val issuer = otp?.issuer ?: entry.category
        if (issuer.isNotBlank()) {
            params["issuer"] = issuer
        }
        // 拼接 URI
        val queryPart = params.map { "${it.key}=${Uri.encode(it.value)}" }.joinToString("&")

        return "otpauth://totp/$label?$queryPart"
    }
}