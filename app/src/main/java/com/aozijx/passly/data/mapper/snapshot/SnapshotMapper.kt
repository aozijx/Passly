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
import com.aozijx.passly.data.model.payload.secret.VaultDataPayload
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

private fun SummaryPayload.toEntrySummary(): EntrySummary = EntrySummary(
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

private fun EntrySecret.toSecretPayload(): SecretPayload = when (this) {
    is EntrySecret.Login -> SecretPayload.Login(
        LoginSecretPayload(
            email = data.email,
            password = data.password,
            notes = data.notes
        )
    )

    is EntrySecret.Note -> SecretPayload.Note(notes)
    is EntrySecret.Card -> SecretPayload.Card(
        CardSecretPayload(
            cardNumber = data.cardNumber,
            cardExpiry = data.cardExpiry,
            cardCvv = data.cardCvv,
            cardHolder = data.cardHolder,
            paymentPin = data.paymentPin,
            paymentPlatform = data.paymentPlatform
        )
    )

    is EntrySecret.Identity -> SecretPayload.Identity(
        IdentitySecretPayload(
            idNumber = data.idNumber,
            securityQuestion = data.securityQuestion,
            securityAnswer = data.securityAnswer,
            seedPhrase = data.seedPhrase,
            recoveryCodes = data.recoveryCodes
        )
    )

    is EntrySecret.SshKey -> SecretPayload.SshKey(
        SshSecretPayload(
            privateKey = data.privateKey,
            publicKey = data.publicKey,
            passphrase = data.passphrase
        )
    )

    is EntrySecret.Wifi -> SecretPayload.Wifi(
        WifiSecretPayload(
            password = data.password,
            securityType = data.securityType,
            isHidden = data.isHidden
        )
    )

    is EntrySecret.Passkey -> SecretPayload.Passkey(
        PasskeySecretPayload(
            credentialId = data.credentialId,
            rpId = data.rpId,
            userHandle = data.userHandle,
            privateKeyReference = data.privateKeyReference,
            hardwareKeyInfo = data.hardwareKeyInfo
        )
    )

    is EntrySecret.Otp -> SecretPayload.Otp(
        OtpSecretPayload(
            config = data.config?.let { otp ->
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
    )

    is EntrySecret.VaultData -> SecretPayload.VaultData(
        VaultDataPayload(
            customFields = data.customFields.map { cf ->
                CustomFieldPayload(
                    name = cf.name,
                    value = cf.value,
                    type = cf.type
                )
            },
            notes = data.notes
        )
    )
}

internal fun SecretPayload.toEntrySecret(): EntrySecret = when (this) {
    is SecretPayload.Login -> EntrySecret.Login(
        LoginSecret(
            email = data.email,
            password = data.password,
            notes = data.notes
        )
    )

    is SecretPayload.Note -> EntrySecret.Note(notes)
    is SecretPayload.Card -> EntrySecret.Card(
        CardSecret(
            cardNumber = data.cardNumber,
            cardExpiry = data.cardExpiry,
            cardCvv = data.cardCvv,
            cardHolder = data.cardHolder,
            paymentPin = data.paymentPin,
            paymentPlatform = data.paymentPlatform
        )
    )

    is SecretPayload.Identity -> EntrySecret.Identity(
        IdentitySecret(
            idNumber = data.idNumber,
            securityQuestion = data.securityQuestion,
            securityAnswer = data.securityAnswer,
            seedPhrase = data.seedPhrase,
            recoveryCodes = data.recoveryCodes
        )
    )

    is SecretPayload.SshKey -> EntrySecret.SshKey(
        SshSecret(
            privateKey = data.privateKey,
            publicKey = data.publicKey,
            passphrase = data.passphrase
        )
    )

    is SecretPayload.Wifi -> EntrySecret.Wifi(
        WifiSecret(
            password = data.password,
            securityType = data.securityType,
            isHidden = data.isHidden
        )
    )

    is SecretPayload.Passkey -> EntrySecret.Passkey(
        PasskeySecret(
            credentialId = data.credentialId,
            rpId = data.rpId,
            userHandle = data.userHandle,
            privateKeyReference = data.privateKeyReference,
            hardwareKeyInfo = data.hardwareKeyInfo
        )
    )

    is SecretPayload.Otp -> EntrySecret.Otp(
        OtpSecret(
            config = data.config?.let { p ->
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
    )

    is SecretPayload.VaultData -> EntrySecret.VaultData(
        customFields = data.data.customFields.map { cf ->
            CustomField(
                name = cf.name,
                value = cf.value,
                type = cf.type
            )
        },
        notes = data.data.notes
    )
}
