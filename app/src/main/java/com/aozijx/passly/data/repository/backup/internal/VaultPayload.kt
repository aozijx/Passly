package com.aozijx.passly.data.repository.backup.internal

import com.aozijx.passly.domain.model.VaultEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val VaultJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
    coerceInputValues = true
}

@Serializable
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
    fun toJson(): String = VaultJson.encodeToString(serializer(), this)

    companion object {
        fun fromJson(jsonString: String): VaultPayload =
            VaultJson.decodeFromString(serializer(), jsonString)
    }
}

fun VaultPayload.toVaultEntry(id: Int, uuid: String, updatedAt: Long): VaultEntry = VaultEntry(
    id = id,
    title = title,
    username = username,
    password = password,
    email = email,
    category = category,
    notes = notes,
    iconName = iconName,
    iconCustomPath = iconCustomPath,
    totpSecret = totpSecret,
    totpIssuer = totpIssuer,
    totpPeriod = totpPeriod,
    totpDigits = totpDigits,
    totpAlgorithm = totpAlgorithm,
    passkeyDataJson = passkeyDataJson,
    recoveryCodes = recoveryCodes,
    hardwareKeyInfo = hardwareKeyInfo,
    wifiSecurityType = wifiSecurityType,
    wifiIsHidden = wifiIsHidden,
    cardCvv = cardCvv,
    cardExpiration = cardExpiration,
    idNumber = idNumber,
    paymentPin = paymentPin,
    paymentPlatform = paymentPlatform,
    securityQuestion = securityQuestion,
    securityAnswer = securityAnswer,
    sshPrivateKey = sshPrivateKey,
    cryptoSeedPhrase = cryptoSeedPhrase,
    entryType = entryType,
    associatedAppPackage = associatedAppPackage,
    associatedDomain = associatedDomain,
    uriList = uriList,
    matchType = matchType,
    customFieldsJson = customFieldsJson,
    autoSubmit = autoSubmit,
    strengthScore = strengthScore,
    lastUsedAt = lastUsedAt,
    usageCount = usageCount,
    favorite = favorite,
    tags = tags,
    uuid = uuid,
    createdAt = createdAt,
    updatedAt = updatedAt,
    expiresAt = expiresAt
)

fun VaultEntry.toVaultPayload(): VaultPayload = VaultPayload(
    title = title,
    username = username,
    password = password,
    email = email,
    category = category,
    notes = notes,
    iconName = iconName,
    iconCustomPath = iconCustomPath,
    totpSecret = totpSecret,
    totpIssuer = totpIssuer,
    totpPeriod = totpPeriod,
    totpDigits = totpDigits,
    totpAlgorithm = totpAlgorithm,
    passkeyDataJson = passkeyDataJson,
    recoveryCodes = recoveryCodes,
    hardwareKeyInfo = hardwareKeyInfo,
    wifiSecurityType = wifiSecurityType,
    wifiIsHidden = wifiIsHidden,
    cardCvv = cardCvv,
    cardExpiration = cardExpiration,
    idNumber = idNumber,
    paymentPin = paymentPin,
    paymentPlatform = paymentPlatform,
    securityQuestion = securityQuestion,
    securityAnswer = securityAnswer,
    sshPrivateKey = sshPrivateKey,
    cryptoSeedPhrase = cryptoSeedPhrase,
    entryType = entryType,
    associatedAppPackage = associatedAppPackage,
    associatedDomain = associatedDomain,
    uriList = uriList,
    matchType = matchType,
    customFieldsJson = customFieldsJson,
    autoSubmit = autoSubmit,
    strengthScore = strengthScore,
    lastUsedAt = lastUsedAt,
    usageCount = usageCount,
    favorite = favorite,
    tags = tags,
    createdAt = createdAt,
    expiresAt = expiresAt
)