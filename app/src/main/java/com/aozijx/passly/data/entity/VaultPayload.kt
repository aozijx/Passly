package com.aozijx.passly.data.entity

import org.json.JSONArray
import org.json.JSONObject

/**
 * Encrypted Blob 的明文载荷：保险库条目的全量业务数据。
 * 序列化为 JSON 后整体加密存入 encrypted_blob 列。
 */
data class VaultPayload(
    val title: String,
    val username: String,
    val password: String,
    val email: String? = null,
    val category: String,
    val notes: String? = null,

    val iconName: String? = null,
    val iconCustomPath: String? = null,

    val totpSecret: String? = null,
    val totpIssuer: String? = null,
    val totpPeriod: Int = 30,
    val totpDigits: Int = 6,
    val totpAlgorithm: String = "SHA1",

    val passkeyDataJson: String? = null,
    val recoveryCodes: String? = null,
    val hardwareKeyInfo: String? = null,

    val wifiSecurityType: String? = "WPA",
    val wifiIsHidden: Boolean = false,

    val cardCvv: String? = null,
    val cardExpiration: String? = null,
    val idNumber: String? = null,

    val paymentPin: String? = null,
    val paymentPlatform: String? = null,

    val securityQuestion: String? = null,
    val securityAnswer: String? = null,

    val sshPrivateKey: String? = null,
    val cryptoSeedPhrase: String? = null,

    val entryType: Int = 0,

    val associatedAppPackage: String? = null,
    val associatedDomain: String? = null,
    val uriList: List<String>? = null,
    val matchType: Int = 0,
    val customFieldsJson: String? = null,
    val autoSubmit: Boolean = false,

    val strengthScore: Float? = null,
    val lastUsedAt: Long? = null,
    val usageCount: Int = 0,

    val favorite: Boolean = false,
    val tags: List<String>? = null,
    val createdAt: Long? = null,
    val expiresAt: Long? = null
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("title", title)
        json.put("username", username)
        json.put("password", password)
        json.putOpt("email", email)
        json.put("category", category)
        json.putOpt("notes", notes)

        json.putOpt("iconName", iconName)
        json.putOpt("iconCustomPath", iconCustomPath)

        json.putOpt("totpSecret", totpSecret)
        json.putOpt("totpIssuer", totpIssuer)
        json.put("totpPeriod", totpPeriod)
        json.put("totpDigits", totpDigits)
        json.put("totpAlgorithm", totpAlgorithm)

        json.putOpt("passkeyDataJson", passkeyDataJson)
        json.putOpt("recoveryCodes", recoveryCodes)
        json.putOpt("hardwareKeyInfo", hardwareKeyInfo)

        json.putOpt("wifiSecurityType", wifiSecurityType)
        json.put("wifiIsHidden", wifiIsHidden)

        json.putOpt("cardCvv", cardCvv)
        json.putOpt("cardExpiration", cardExpiration)
        json.putOpt("idNumber", idNumber)

        json.putOpt("paymentPin", paymentPin)
        json.putOpt("paymentPlatform", paymentPlatform)

        json.putOpt("securityQuestion", securityQuestion)
        json.putOpt("securityAnswer", securityAnswer)

        json.putOpt("sshPrivateKey", sshPrivateKey)
        json.putOpt("cryptoSeedPhrase", cryptoSeedPhrase)

        json.put("entryType", entryType)

        json.putOpt("associatedAppPackage", associatedAppPackage)
        json.putOpt("associatedDomain", associatedDomain)
        uriList?.let { json.put("uriList", JSONArray(it)) }
        json.put("matchType", matchType)
        json.putOpt("customFieldsJson", customFieldsJson)
        json.put("autoSubmit", autoSubmit)

        strengthScore?.let { json.put("strengthScore", it.toDouble()) }
        lastUsedAt?.let { json.put("lastUsedAt", it) }
        json.put("usageCount", usageCount)

        json.put("favorite", favorite)
        tags?.let { json.put("tags", JSONArray(it)) }
        createdAt?.let { json.put("createdAt", it) }
        expiresAt?.let { json.put("expiresAt", it) }

        return json.toString()
    }

    companion object {
        fun fromJson(jsonString: String): VaultPayload {
            val json = JSONObject(jsonString)
            return VaultPayload(
                title = json.getString("title"),
                username = json.getString("username"),
                password = json.getString("password"),
                email = json.optStringOrNull("email"),
                category = json.getString("category"),
                notes = json.optStringOrNull("notes"),

                iconName = json.optStringOrNull("iconName"),
                iconCustomPath = json.optStringOrNull("iconCustomPath"),

                totpSecret = json.optStringOrNull("totpSecret"),
                totpIssuer = json.optStringOrNull("totpIssuer"),
                totpPeriod = json.optInt("totpPeriod", 30),
                totpDigits = json.optInt("totpDigits", 6),
                totpAlgorithm = json.optString("totpAlgorithm", "SHA1"),

                passkeyDataJson = json.optStringOrNull("passkeyDataJson"),
                recoveryCodes = json.optStringOrNull("recoveryCodes"),
                hardwareKeyInfo = json.optStringOrNull("hardwareKeyInfo"),

                wifiSecurityType = json.optString("wifiSecurityType", "WPA"),
                wifiIsHidden = json.optBoolean("wifiIsHidden", false),

                cardCvv = json.optStringOrNull("cardCvv"),
                cardExpiration = json.optStringOrNull("cardExpiration"),
                idNumber = json.optStringOrNull("idNumber"),

                paymentPin = json.optStringOrNull("paymentPin"),
                paymentPlatform = json.optStringOrNull("paymentPlatform"),

                securityQuestion = json.optStringOrNull("securityQuestion"),
                securityAnswer = json.optStringOrNull("securityAnswer"),

                sshPrivateKey = json.optStringOrNull("sshPrivateKey"),
                cryptoSeedPhrase = json.optStringOrNull("cryptoSeedPhrase"),

                entryType = json.optInt("entryType", 0),

                associatedAppPackage = json.optStringOrNull("associatedAppPackage"),
                associatedDomain = json.optStringOrNull("associatedDomain"),
                uriList = json.optJSONArray("uriList")?.toStringList(),
                matchType = json.optInt("matchType", 0),
                customFieldsJson = json.optStringOrNull("customFieldsJson"),
                autoSubmit = json.optBoolean("autoSubmit", false),

                strengthScore = if (json.has("strengthScore")) json.getDouble("strengthScore").toFloat() else null,
                lastUsedAt = if (json.has("lastUsedAt")) json.getLong("lastUsedAt") else null,
                usageCount = json.optInt("usageCount", 0),

                favorite = json.optBoolean("favorite", false),
                tags = json.optJSONArray("tags")?.toStringList(),
                createdAt = if (json.has("createdAt")) json.getLong("createdAt") else null,
                expiresAt = if (json.has("expiresAt")) json.getLong("expiresAt") else null
            )
        }

        private fun JSONObject.optStringOrNull(key: String): String? =
            if (has(key) && !isNull(key)) getString(key) else null

        private fun JSONArray.toStringList(): List<String> =
            (0 until length()).map { getString(it) }
    }
}
