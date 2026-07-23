package com.aozijx.passly.data.mapper.snapshot

import com.aozijx.passly.data.model.payload.secret.CardSecretPayload
import com.aozijx.passly.data.model.payload.secret.CustomFieldPayload
import com.aozijx.passly.data.model.payload.secret.IdentitySecretPayload
import com.aozijx.passly.data.model.payload.secret.LoginSecretPayload
import com.aozijx.passly.data.model.payload.secret.OtpConfigPayload
import com.aozijx.passly.data.model.payload.secret.OtpHashAlgorithmPayload
import com.aozijx.passly.data.model.payload.secret.OtpSecretEncodingPayload
import com.aozijx.passly.data.model.payload.secret.OtpSecretPayload
import com.aozijx.passly.data.model.payload.secret.OtpTypePayload
import com.aozijx.passly.data.model.payload.secret.PasskeySecretPayload
import com.aozijx.passly.data.model.payload.secret.SecretPayload
import com.aozijx.passly.data.model.payload.secret.SshSecretPayload
import com.aozijx.passly.data.model.payload.secret.WifiSecretPayload
import com.aozijx.passly.data.model.payload.snapshot.VaultSnapshot
import com.aozijx.passly.data.model.payload.summary.SummaryPayload
import com.aozijx.passly.data.model.payload.summary.WebsiteInfoPayload
import com.aozijx.passly.domain.model.entry.EntryHeader
import com.aozijx.passly.domain.model.entry.EntryId
import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.EntrySummary
import com.aozijx.passly.domain.model.entry.EntryVersion
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.WebsiteInfo
import com.aozijx.passly.domain.model.entry.secret.CardSecret
import com.aozijx.passly.domain.model.entry.secret.CustomField
import com.aozijx.passly.domain.model.entry.secret.IdentitySecret
import com.aozijx.passly.domain.model.entry.secret.LoginSecret
import com.aozijx.passly.domain.model.entry.secret.OtpSecret
import com.aozijx.passly.domain.model.entry.secret.PasskeySecret
import com.aozijx.passly.domain.model.entry.secret.SshSecret
import com.aozijx.passly.domain.model.entry.secret.WifiSecret
import com.aozijx.passly.domain.model.otp.OtpConfig
import com.aozijx.passly.domain.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.model.otp.OtpType

fun VaultEntry.toSnapshot(): VaultSnapshot = VaultSnapshot(
    id = id,
    entryType = entryType,
    revision = entryVersion,
    deletedAt = deletedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    summary = summary.toSummaryPayload(),
    secret = secret.toSecretPayload()
)

fun VaultSnapshot.toDomain(): VaultEntry = VaultEntry(
    header = EntryHeader(
        id = EntryId(id),
        entryType = entryType,
        version = EntryVersion.fromInt(revision),
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    ),
    summary = summary.toEntrySummary(),
    secret = secret.toEntrySecret()
)

// --- Summary conversion ---

private fun EntrySummary.toSummaryPayload(): SummaryPayload = SummaryPayload(
    title = title,
    username = username,
    website = website?.let { w ->
        WebsiteInfoPayload(
            primaryUrl = w.primaryUrl,
            matchDomains = w.matchDomains,
            packageNames = w.packageNames
        )
    },
    icon = icon,
    iconCustomPath = iconCustomPath,
    favorite = favorite,
    tags = tags,
    color = color,
    expiresAt = expiresAt
)

internal fun SummaryPayload.toEntrySummary(): EntrySummary = EntrySummary(
    title = title,
    username = username,
    website = website?.let { w ->
        WebsiteInfo(
            primaryUrl = w.primaryUrl,
            matchDomains = w.matchDomains,
            packageNames = w.packageNames
        )
    },
    icon = icon,
    iconCustomPath = iconCustomPath,
    favorite = favorite,
    tags = tags,
    color = color,
    expiresAt = expiresAt
)

// --- Secret conversion ---

private fun EntrySecret.toSecretPayload(): SecretPayload = SecretPayload(
    login = login?.let { LoginSecretPayload(email = it.email, password = it.password) },
    notes = notes,
    card = card?.let {
        CardSecretPayload(
            cardNumber = it.cardNumber,
            cardExpiry = it.cardExpiry,
            cardCvv = it.cardCvv,
            cardHolder = it.cardHolder,
            paymentPin = it.paymentPin,
            paymentPlatform = it.paymentPlatform
        )
    },
    identity = identity?.let {
        IdentitySecretPayload(
            idNumber = it.idNumber,
            securityQuestion = it.securityQuestion,
            securityAnswer = it.securityAnswer,
            seedPhrase = it.seedPhrase,
            recoveryCodes = it.recoveryCodes
        )
    },
    ssh = ssh?.let {
        SshSecretPayload(
            privateKey = it.privateKey,
            publicKey = it.publicKey,
            passphrase = it.passphrase
        )
    },
    wifi = wifi?.let {
        WifiSecretPayload(
            password = it.password,
            securityType = it.securityType,
            isHidden = it.isHidden
        )
    },
    passkey = passkey?.let {
        PasskeySecretPayload(
            credentialId = it.credentialId,
            rpId = it.rpId,
            userHandle = it.userHandle,
            privateKeyReference = it.privateKeyReference,
            hardwareKeyInfo = it.hardwareKeyInfo
        )
    },
    otp = otp?.let {
        OtpSecretPayload(
            config = it.config?.let { otp ->
                OtpConfigPayload(
                    type = OtpTypePayload.valueOf(otp.type.name),
                    secret = otp.secret,
                    algorithm = OtpHashAlgorithmPayload.valueOf(otp.algorithm.name),
                    digits = otp.digits,
                    periodSeconds = otp.periodSeconds,
                    counter = otp.counter,
                    encoding = OtpSecretEncodingPayload.valueOf(otp.encoding.name),
                    issuer = otp.issuer,
                    accountName = otp.accountName
                )
            }
        )
    },
    customFields = customFields.map { cf ->
        CustomFieldPayload(
            name = cf.name,
            value = cf.value,
            type = cf.type
        )
    }
)

internal fun SecretPayload.toEntrySecret(): EntrySecret = EntrySecret(
    login = login?.let { LoginSecret(email = it.email, password = it.password) },
    notes = notes,
    card = card?.let {
        CardSecret(
            cardNumber = it.cardNumber,
            cardExpiry = it.cardExpiry,
            cardCvv = it.cardCvv,
            cardHolder = it.cardHolder,
            paymentPin = it.paymentPin,
            paymentPlatform = it.paymentPlatform
        )
    },
    identity = identity?.let {
        IdentitySecret(
            idNumber = it.idNumber,
            securityQuestion = it.securityQuestion,
            securityAnswer = it.securityAnswer,
            seedPhrase = it.seedPhrase,
            recoveryCodes = it.recoveryCodes
        )
    },
    ssh = ssh?.let {
        SshSecret(
            privateKey = it.privateKey,
            publicKey = it.publicKey,
            passphrase = it.passphrase
        )
    },
    wifi = wifi?.let {
        WifiSecret(
            password = it.password,
            securityType = it.securityType,
            isHidden = it.isHidden
        )
    },
    passkey = passkey?.let {
        PasskeySecret(
            credentialId = it.credentialId,
            rpId = it.rpId,
            userHandle = it.userHandle,
            privateKeyReference = it.privateKeyReference,
            hardwareKeyInfo = it.hardwareKeyInfo
        )
    },
    otp = otp?.let {
        OtpSecret(
            config = it.config?.let { p ->
                OtpConfig(
                    type = OtpType.valueOf(p.type.name),
                    secret = p.secret,
                    algorithm = OtpHashAlgorithm.valueOf(p.algorithm.name),
                    digits = p.digits,
                    periodSeconds = p.periodSeconds,
                    counter = p.counter,
                    encoding = OtpSecretEncoding.valueOf(p.encoding.name),
                    issuer = p.issuer,
                    accountName = p.accountName
                )
            }
        )
    },
    customFields = customFields.map { cf ->
        CustomField(
            name = cf.name,
            value = cf.value,
            type = cf.type
        )
    }
)
