package com.aozijx.passly.data.repository.backup.internal

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.aozijx.passly.data.entity.VaultPayload
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
        writer.name("category").value(p.category)
        writer.name("notes").value(p.notes)
        writer.name("iconName").value(p.iconName)
        writer.name("iconCustomPath").value(p.iconCustomPath)
        writer.name("totpSecret").value(p.totpSecret)
        writer.name("totpPeriod").value(p.totpPeriod.toLong())
        writer.name("totpDigits").value(p.totpDigits.toLong())
        writer.name("totpAlgorithm").value(p.totpAlgorithm)
        writer.name("passkeyDataJson").value(p.passkeyDataJson)
        writer.name("recoveryCodes").value(p.recoveryCodes)
        writer.name("hardwareKeyInfo").value(p.hardwareKeyInfo)
        writer.name("wifiEncryptionType").value(p.wifiSecurityType)
        writer.name("wifiIsHidden").value(p.wifiIsHidden)
        writer.name("cardCvv").value(p.cardCvv)
        writer.name("cardExpiration").value(p.cardExpiration)
        writer.name("idNumber").value(p.idNumber)
        writer.name("paymentPin").value(p.paymentPin)
        writer.name("paymentPlatform").value(p.paymentPlatform)
        writer.name("securityQuestion").value(p.securityQuestion)
        writer.name("securityAnswer").value(p.securityAnswer)
        writer.name("sshPrivateKey").value(p.sshPrivateKey)
        writer.name("cryptoSeedPhrase").value(p.cryptoSeedPhrase)
        writer.name("entryType").value(p.entryType.toLong())
        writer.name("associatedAppPackage").value(p.associatedAppPackage)
        writer.name("associatedDomain").value(p.associatedDomain)
        writer.name("uriList")
        if (p.uriList == null) writer.nullValue() else {
            writer.beginArray()
            p.uriList.forEach { writer.value(it) }
            writer.endArray()
        }
        writer.name("matchType").value(p.matchType.toLong())
        writer.name("customFieldsJson").value(p.customFieldsJson)
        writer.name("autoSubmit").value(p.autoSubmit)
        writer.name("strengthScore").value(p.strengthScore?.toDouble())
        writer.name("lastUsedAt").value(p.lastUsedAt)
        writer.name("usageCount").value(p.usageCount.toLong())
        writer.name("favorite").value(p.favorite)
        writer.name("tags")
        if (p.tags == null) writer.nullValue() else {
            writer.beginArray()
            p.tags.forEach { writer.value(it) }
            writer.endArray()
        }
        writer.name("createdAt").value(p.createdAt)
        writer.name("expiresAt").value(p.expiresAt)
        writer.endObject()
    }

    private fun readPayload(reader: JsonReader): VaultPayload {
        var title = ""
        var username = ""
        var password = ""
        var category = ""
        var notes: String? = null
        var iconName: String? = null
        var iconCustomPath: String? = null
        var totpSecret: String? = null
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
        var createdAt: Long? = System.currentTimeMillis()
        var expiresAt: Long? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "title" -> title = reader.nextString()
                "username" -> username = reader.nextString()
                "password" -> password = reader.nextString()
                "category" -> category = reader.nextString()
                "notes" -> notes = reader.nextNullableString()
                "iconName" -> iconName = reader.nextNullableString()
                "iconCustomPath" -> iconCustomPath = reader.nextNullableString()
                "totpSecret" -> totpSecret = reader.nextNullableString()
                "totpPeriod" -> totpPeriod = reader.nextInt()
                "totpDigits" -> totpDigits = reader.nextInt()
                "totpAlgorithm" -> totpAlgorithm = reader.nextString()
                "passkeyDataJson" -> passkeyDataJson = reader.nextNullableString()
                "recoveryCodes" -> recoveryCodes = reader.nextNullableString()
                "hardwareKeyInfo" -> hardwareKeyInfo = reader.nextNullableString()
                "wifiEncryptionType" -> wifiSecurityType = reader.nextNullableString()
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
            category = category,
            notes = notes,
            iconName = iconName,
            iconCustomPath = iconCustomPath,
            totpSecret = totpSecret,
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
