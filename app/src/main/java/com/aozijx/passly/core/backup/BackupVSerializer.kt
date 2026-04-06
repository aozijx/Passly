package com.aozijx.passly.core.backup

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.aozijx.passly.data.entity.VaultEntryEntity
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

internal object BackupVSerializer {
    /**
     * 注意：此方法不会关闭传入的 output 流，因为在 ZIP 导出中，
     * 写入一个 Entry 后流必须保持开启以继续写入后续 Entry 或 ZIP 中央目录。
     */
    fun writeEntries(output: OutputStream, entries: List<VaultEntryEntity>) {
        val writer = JsonWriter(OutputStreamWriter(output, StandardCharsets.UTF_8))
        writer.setIndent("")
        writer.beginArray()
        entries.forEach { writeEntry(writer, it) }
        writer.endArray()
        // 必须手动 flush 以确保数据写入底层的 ZipOutputStream，
        // 但严禁调用 close()，否则会导致外层 ZipOutputStream 提前关闭。
        writer.flush()
    }

    /**
     * 注意：此方法不会关闭传入的 input 流。
     */
    fun readEntries(input: InputStream): List<VaultEntryEntity> {
        val entries = mutableListOf<VaultEntryEntity>()
        val reader = JsonReader(InputStreamReader(input, StandardCharsets.UTF_8))
        reader.beginArray()
        while (reader.hasNext()) entries.add(readEntry(reader))
        reader.endArray()
        return entries
    }

    private fun writeEntry(writer: JsonWriter, entry: VaultEntryEntity) {
        writer.beginObject()
        writer.name("title").value(entry.title)
        writer.name("username").value(entry.username)
        writer.name("password").value(entry.password)
        writer.name("category").value(entry.category)
        writer.name("notes").value(entry.notes)
        writer.name("iconName").value(entry.iconName)
        writer.name("iconCustomPath").value(entry.iconCustomPath)
        writer.name("totpSecret").value(entry.totpSecret)
        writer.name("totpPeriod").value(entry.totpPeriod.toLong())
        writer.name("totpDigits").value(entry.totpDigits.toLong())
        writer.name("totpAlgorithm").value(entry.totpAlgorithm)
        writer.name("passkeyDataJson").value(entry.passkeyDataJson)
        writer.name("recoveryCodes").value(entry.recoveryCodes)
        writer.name("hardwareKeyInfo").value(entry.hardwareKeyInfo)
        writer.name("wifiEncryptionType").value(entry.wifiSecurityType)
        writer.name("wifiIsHidden").value(entry.wifiIsHidden)
        writer.name("cardCvv").value(entry.cardCvv)
        writer.name("cardExpiration").value(entry.cardExpiration)
        writer.name("idNumber").value(entry.idNumber)
        writer.name("paymentPin").value(entry.paymentPin)
        writer.name("paymentPlatform").value(entry.paymentPlatform)
        writer.name("securityQuestion").value(entry.securityQuestion)
        writer.name("securityAnswer").value(entry.securityAnswer)
        writer.name("sshPrivateKey").value(entry.sshPrivateKey)
        writer.name("cryptoSeedPhrase").value(entry.cryptoSeedPhrase)
        writer.name("entryType").value(entry.entryType.toLong())
        writer.name("associatedAppPackage").value(entry.associatedAppPackage)
        writer.name("associatedDomain").value(entry.associatedDomain)
        writer.name("uriList")
        if (entry.uriList == null) writer.nullValue() else {
            writer.beginArray()
            entry.uriList.forEach { writer.value(it) }
            writer.endArray()
        }
        writer.name("matchType").value(entry.matchType.toLong())
        writer.name("customFieldsJson").value(entry.customFieldsJson)
        writer.name("autoSubmit").value(entry.autoSubmit)
        writer.name("strengthScore").value(entry.strengthScore?.toDouble())
        writer.name("lastUsedAt").value(entry.lastUsedAt)
        writer.name("usageCount").value(entry.usageCount.toLong())
        writer.name("favorite").value(entry.favorite)
        writer.name("tags")
        if (entry.tags == null) writer.nullValue() else {
            writer.beginArray()
            entry.tags.forEach { writer.value(it) }
            writer.endArray()
        }
        writer.name("createdAt").value(entry.createdAt)
        writer.name("updatedAt").value(entry.updatedAt)
        writer.name("expiresAt").value(entry.expiresAt)
        writer.endObject()
    }

    private fun readEntry(reader: JsonReader): VaultEntryEntity {
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
        var updatedAt: Long? = null
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
                "updatedAt" -> updatedAt = reader.nextNullableLong()
                "expiresAt" -> expiresAt = reader.nextNullableLong()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return VaultEntryEntity(
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
            updatedAt = updatedAt,
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
