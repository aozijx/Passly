package com.aozijx.passly.core.otp

import android.net.Uri
import androidx.core.net.toUri
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import java.net.URLDecoder

/**
 * otpauth:// URI 解析与构建工具。
 *
 * 支持：
 * - otpauth://totp/...  (RFC 6238)
 * - otpauth://hotp/...  (RFC 4226)
 * - algorithm=SHA1|SHA256|SHA512
 * - digits, period, counter, issuer, secret 等参数
 */
object OtpAuthUriCodec {

    /**
     * 解析 otpauth:// URI 为 [OtpConfig]。
     *
     * @return 解析成功返回 [OtpConfig]，失败返回 null
     */
    fun parse(uriString: String): OtpConfig? {
        if (uriString.isBlank() || !uriString.startsWith("otpauth://")) return null
        return try {
            val uri = uriString.toUri()
            val type = when (uri.host?.lowercase()) {
                "totp" -> OtpType.TOTP
                "hotp" -> OtpType.HOTP
                else -> return null
            }
            val secret = uri.getQueryParameter("secret")
                ?.replace(" ", "")
                ?.uppercase()
                ?: return null
            val rawLabel = URLDecoder.decode(uri.path?.trimStart('/') ?: "", "UTF-8")
            val issuerParam = uri.getQueryParameter("issuer")
            val rawAlgorithm = uri.getQueryParameter("algorithm")?.uppercase()

            // 分离 label 中的 issuer:accountName 格式
            val (issuer, accountName) = if (issuerParam != null) {
                issuerParam to rawLabel.removePrefix("$issuerParam:")
            } else {
                val colonIndex = rawLabel.indexOf(':')
                if (colonIndex > 0) {
                    rawLabel.take(colonIndex) to rawLabel.substring(colonIndex + 1)
                } else {
                    null to rawLabel
                }
            }

            val algorithm = when (rawAlgorithm) {
                "SHA256" -> OtpHashAlgorithm.SHA256
                "SHA512" -> OtpHashAlgorithm.SHA512
                else -> OtpHashAlgorithm.SHA1
            }

            val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6
            val periodSeconds = uri.getQueryParameter("period")?.toIntOrNull()
            val counter = uri.getQueryParameter("counter")?.toLongOrNull()

            // otpauth URI 的 secret 按规范是 Base32；不在生成阶段猜测编码。
            val encoding = OtpSecretEncoding.BASE32

            // Steam 的 otpauth URI 使用 Steam issuer/label 前缀作为明确类型标记。
            val labelIssuer = rawLabel.substringBefore(':', missingDelimiterValue = "")
            val resolvedType = if (
                type == OtpType.TOTP &&
                (issuer.equals("Steam", ignoreCase = true) ||
                        labelIssuer.equals("Steam", ignoreCase = true))
            ) OtpType.STEAM else type

            val resolvedDigits = if (resolvedType == OtpType.STEAM) 5 else digits

            OtpConfig(
                type = resolvedType,
                secret = secret,
                algorithm = algorithm,
                digits = resolvedDigits,
                periodSeconds = if (resolvedType == OtpType.HOTP) null else (periodSeconds ?: 30),
                counter = if (resolvedType == OtpType.HOTP) (counter ?: 0L) else null,
                encoding = encoding,
                issuer = issuer,
                accountName = accountName
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 将 [OtpConfig] 构建为 otpauth:// URI。
     */
    fun format(config: OtpConfig, title: String): String {
        val host = when (config.type) {
            OtpType.TOTP, OtpType.STEAM -> "totp"
            OtpType.HOTP -> "hotp"
        }
        val label = Uri.encode(title, "UTF-8")
        val encodedSecret = when (config.encoding) {
            OtpSecretEncoding.BASE32 -> config.secret
                ?.replace(" ", "")?.uppercase() ?: error("OTP secret is not decrypted")
            OtpSecretEncoding.BASE64 -> config.secret?.trim() ?: error("OTP secret is not decrypted")
        }
        val params = mutableMapOf(
            "secret" to encodedSecret,
            "algorithm" to config.algorithm.name
        )
        if (config.type != OtpType.STEAM) {
            params["digits"] = config.digits.toString()
        }
        if (config.type != OtpType.HOTP) {
            params["period"] = (config.periodSeconds ?: 30).toString()
        } else {
            params["counter"] = (config.counter ?: 0L).toString()
        }
        val issuer = config.issuer ?: if (config.type == OtpType.STEAM) "Steam" else null
        if (!issuer.isNullOrBlank()) {
            params["issuer"] = Uri.encode(issuer)
        }
        val queryPart = params.map { "${it.key}=${Uri.encode(it.value)}" }.joinToString("&")
        return "otpauth://$host/$label?$queryPart"
    }
}
