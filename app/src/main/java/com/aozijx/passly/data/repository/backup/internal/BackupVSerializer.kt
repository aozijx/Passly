package com.aozijx.passly.data.repository.backup.internal

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

internal object BackupVSerializer {

    fun writeEntries(output: OutputStream, payloads: List<VaultPayload>) {
        val writer = JsonWriter(OutputStreamWriter(output, StandardCharsets.UTF_8))
        writer.setIndent("")
        writer.beginArray()
        payloads.forEach { writePayload(writer, it) }
        writer.endArray()
        writer.flush()
    }

    fun readEntries(input: InputStream): List<VaultPayload> {
        val payloads = mutableListOf<VaultPayload>()
        val reader = JsonReader(InputStreamReader(input, StandardCharsets.UTF_8))
        reader.beginArray()
        while (reader.hasNext()) payloads.add(readPayload(reader))
        reader.endArray()
        return payloads
    }

    private fun writePayload(writer: JsonWriter, p: VaultPayload) {
        writer.beginObject()
        writer.name("title").value(p.title)
        writer.name("username").value(p.username)
        writer.name("password").value(p.password)
        p.email?.let { writer.name("email").value(it) }
        writer.name("category").value(p.category)
        
        p.notes?.let { writer.name("notes").value(it) }
        p.iconName?.let { writer.name("iconName").value(it) }
        p.iconCustomPath?.let { writer.name("iconCustomPath").value(it) }
        
        p.totpSecret?.let {
            writer.name("totpSecret").value(it)
            p.totpIssuer?.let { issuer -> writer.name("totpIssuer").value(issuer) }
            writer.name("totpPeriod").value(p.totpPeriod.toLong())
            writer.name("totpDigits").value(p.totpDigits.toLong())
            writer.name("totpAlgorithm").value(p.totpAlgorithm)
        }

        p.passkeyDataJson?.let { writer.name("passkeyDataJson").value(it) }
        p.recoveryCodes?.let { writer.name("recoveryCodes").value(it) }
        p.hardwareKeyInfo?.let { writer.name("hardwareKeyInfo").value(it) }
        
        p.wifiSecurityType?.let { writer.name("wifiSecurityType").value(it) }
        if (p.wifiIsHidden) writer.name("wifiIsHidden").value(true)

        p.cardCvv?.let { writer.name("cardCvv").value(it) }
        p.cardExpiration?.let { writer.name("cardExpiration").value(it) }
        p.idNumber?.let { writer.name("idNumber").value(it) }
        
        p.paymentPin?.let { writer.name("paymentPin").value(it) }
        p.paymentPlatform?.let { writer.name("paymentPlatform").value(it) }
        
        p.securityQuestion?.let { writer.name("securityQuestion").value(it) }
        p.securityAnswer?.let { writer.name("securityAnswer").value(it) }
        
        p.sshPrivateKey?.let { writer.name("sshPrivateKey").value(it) }
        p.cryptoSeedPhrase?.let { writer.name("cryptoSeedPhrase").value(it) }
        
        writer.name("entryType").value(p.entryType.toLong())
        
        p.associatedAppPackage?.let { writer.name("associatedAppPackage").value(it) }
        p.associatedDomain?.let { writer.name("associatedDomain").value(it) }
        
        p.uriList?.let { list ->
            if (list.isNotEmpty()) {
                writer.name("uriList")
                writer.beginArray()
                list.forEach { writer.value(it) }
                writer.endArray()
            }
        }
        
        if (p.matchType != 0) writer.name("matchType").value(p.matchType.toLong())
        p.customFieldsJson?.let { writer.name("customFieldsJson").value(it) }
        if (p.autoSubmit) writer.name("autoSubmit").value(true)
        
        p.strengthScore?.let { writer.name("strengthScore").value(it.toDouble()) }
        p.lastUsedAt?.let { writer.name("lastUsedAt").value(it) }
        if (p.usageCount != 0) writer.name("usageCount").value(p.usageCount.toLong())
        if (p.favorite) writer.name("favorite").value(true)
        
        p.tags?.let { list ->
            if (list.isNotEmpty()) {
                writer.name("tags")
                writer.beginArray()
                list.forEach { writer.value(it) }
                writer.endArray()
            }
        }
        
        p.createdAt?.let { writer.name("createdAt").value(it) }
        p.expiresAt?.let { writer.name("expiresAt").value(it) }
        
        writer.endObject()
    }

    private fun readPayload(reader: JsonReader): VaultPayload {
        var title = ""
        var username = ""
        var password = ""
        var email: String? = null
        var category = ""
        var notes: String? = null
        var iconName: String? = null
        var iconCustomPath: String? = null
        var totpSecret: String? = null
        var totpIssuer: String? = null
        var totpPeriod = 30
        var totpDigits = 6
        var totpAlgorithm = "SHA1"
        var passkeyDataJson: String? = null
        var recoveryCodes: String? = null
        var hardwareKeyInfo: String? = null
        var wifiSecurityType: String? = "WPA"
        var wifiIsHidden = false
        var cardCvv: String? = null
        var cardExpiration: String? = null
        var idNumber: String? = null
        var paymentPin: String? = null
        var paymentPlatform: String? = null
        var securityQuestion: String? = null
        var securityAnswer: String? = null
        var sshPrivateKey: String? = null
        var cryptoSeedPhrase: String? = null
        var entryType = 0
        var associatedAppPackage: String? = null
        var associatedDomain: String? = null
        var uriList: List<String>? = null
        var matchType = 0
        var customFieldsJson: String? = null
        var autoSubmit = false
        var strengthScore: Float? = null
        var lastUsedAt: Long? = null
        var usageCount = 0
        var favorite = false
        var tags: List<String>? = null
        var createdAt: Long? = null
        var expiresAt: Long? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "title" -> title = reader.nextString()
                "username" -> username = reader.nextString()
                "password" -> password = reader.nextString()
                "email" -> email = reader.nextNullableString()
                "category" -> category = reader.nextString()
                "notes" -> notes = reader.nextNullableString()
                "iconName" -> iconName = reader.nextNullableString()
                "iconCustomPath" -> iconCustomPath = reader.nextNullableString()
                "totpSecret" -> totpSecret = reader.nextNullableString()
                "totpIssuer" -> totpIssuer = reader.nextNullableString()
                "totpPeriod" -> totpPeriod = reader.nextInt()
                "totpDigits" -> totpDigits = reader.nextInt()
                "totpAlgorithm" -> totpAlgorithm = reader.nextString()
                "passkeyDataJson" -> passkeyDataJson = reader.nextNullableString()
                "recoveryCodes" -> recoveryCodes = reader.nextNullableString()
                "hardwareKeyInfo" -> hardwareKeyInfo = reader.nextNullableString()
                "wifiSecurityType", "wifiEncryptionType" -> wifiSecurityType = reader.nextNullableString()
                "wifiIsHidden" -> wifiIsHidden = reader.nextBoolean()
                "cardCvv" -> cardCvv = reader.nextNullableString()
                "cardExpiration" -> cardExpiration = reader.nextNullableString()
                "idNumber" -> idNumber = reader.nextNullableString()
                "paymentPin" -> paymentPin = reader.nextNullableString()
                "paymentPlatform" -> paymentPlatform = reader.nextNullableString()
                "securityQuestion" -> securityQuestion = reader.nextNullableString()
                "securityAnswer" -> securityAnswer = reader.nextNullableString()
                "sshPrivateKey" -> sshPrivateKey = reader.nextNullableString()
                "cryptoSeedPhrase" -> cryptoSeedPhrase = reader.nextNullableString()
                "entryType" -> entryType = reader.nextInt()
                "associatedAppPackage" -> associatedAppPackage = reader.nextNullableString()
                "associatedDomain" -> associatedDomain = reader.nextNullableString()
                "uriList" -> uriList = reader.nextStringList()
                "matchType" -> matchType = reader.nextInt()
                "customFieldsJson" -> customFieldsJson = reader.nextNullableString()
                "autoSubmit" -> autoSubmit = reader.nextBoolean()
                "strengthScore" -> strengthScore = reader.nextNullableDouble()?.toFloat()
                "lastUsedAt" -> lastUsedAt = reader.nextNullableLong()
                "usageCount" -> usageCount = reader.nextInt()
                "favorite" -> favorite = reader.nextBoolean()
                "tags" -> tags = reader.nextStringList()
                "createdAt" -> createdAt = reader.nextNullableLong()
                "expiresAt" -> expiresAt = reader.nextNullableLong()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return VaultPayload(
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
    }

    private fun JsonReader.nextNullableString(): String? = if (peek() == JsonToken.NULL) {
        skipValue(); null
    } else nextString()

    private fun JsonReader.nextNullableDouble(): Double? = if (peek() == JsonToken.NULL) {
        skipValue(); null
    } else nextDouble()

    private fun JsonReader.nextNullableLong(): Long? = if (peek() == JsonToken.NULL) {
        skipValue(); null
    } else nextLong()

    private fun JsonReader.nextStringList(): List<String>? {
        if (peek() == JsonToken.NULL) {
            skipValue()
            return null
        }
        val list = mutableListOf<String>()
        beginArray()
        while (hasNext()) list.add(nextString())
        endArray()
        return list
    }
}