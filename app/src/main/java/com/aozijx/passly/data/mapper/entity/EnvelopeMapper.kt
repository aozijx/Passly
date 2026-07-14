package com.aozijx.passly.data.mapper.entity

import com.aozijx.passly.data.model.entity.KeyEnvelopeEntity
import com.aozijx.passly.domain.model.envelope.EnvelopeType
import com.aozijx.passly.domain.model.envelope.KdfAlgorithm
import com.aozijx.passly.security.envelope.Envelope
import com.aozijx.passly.security.envelope.KdfParams

private fun EnvelopeType.toInt(): Int = when (this) {
    EnvelopeType.BIOMETRIC -> 0
    EnvelopeType.DEVICE_CREDENTIAL -> 1
    EnvelopeType.APP_PASSWORD -> 2
    EnvelopeType.RECOVERY -> 3
    EnvelopeType.PASSKEY -> 4
    EnvelopeType.YUBIKEY -> 5
    EnvelopeType.ENTERPRISE -> 6
    else -> 0
}

private fun Int.toEnvelopeType(): EnvelopeType = when (this) {
    0 -> EnvelopeType.BIOMETRIC
    1 -> EnvelopeType.DEVICE_CREDENTIAL
    2 -> EnvelopeType.APP_PASSWORD
    3 -> EnvelopeType.RECOVERY
    4 -> EnvelopeType.PASSKEY
    5 -> EnvelopeType.YUBIKEY
    6 -> EnvelopeType.ENTERPRISE
    else -> EnvelopeType("unknown_$this")
}

private fun KdfAlgorithm.toInt(): Int = when (this) {
    KdfAlgorithm.NONE -> 0
    KdfAlgorithm.ARGON2ID -> 1
    KdfAlgorithm.PBKDF2 -> 2
    else -> 0
}

private fun Int.toKdfAlgorithm(): KdfAlgorithm = when (this) {
    0 -> KdfAlgorithm.NONE
    1 -> KdfAlgorithm.ARGON2ID
    2 -> KdfAlgorithm.PBKDF2
    else -> KdfAlgorithm("unknown_$this")
}

fun Envelope.toEntity(): KeyEnvelopeEntity {
    val kdfSalt = kdfParams?.salt
    return KeyEnvelopeEntity(
        envelopeId = id,
        type = type.toInt(),
        algorithm = kdfParams?.algorithm?.toInt() ?: 0,
        ciphertext = dekCiphertext,
        kdfSalt = kdfSalt,
        createdAt = createdAt,
        updatedAt = createdAt
    )
}

fun KeyEnvelopeEntity.toDomain(): Envelope {
    val type = type.toEnvelopeType()
    val kdfParams = if (kdfSalt != null && algorithm != 0) {
        KdfParams(
            algorithm = algorithm.toKdfAlgorithm(),
            salt = kdfSalt
        )
    } else {
        null
    }
    return Envelope(
        id = envelopeId,
        type = type,
        dekCiphertext = ciphertext,
        iv = byteArrayOf(),
        kdfParams = kdfParams,
        createdAt = createdAt
    )
}