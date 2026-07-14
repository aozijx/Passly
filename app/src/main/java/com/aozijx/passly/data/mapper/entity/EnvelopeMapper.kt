package com.aozijx.passly.data.mapper.entity

import com.aozijx.passly.data.model.entity.KeyEnvelopeEntity
import com.aozijx.passly.security.envelope.Envelope
import com.aozijx.passly.security.envelope.EnvelopeType
import com.aozijx.passly.security.envelope.KdfParams

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
    val type = EnvelopeType.fromInt(type)
    val kdfParams = if (kdfSalt != null && algorithm != 0) {
        KdfParams(
            algorithm = KdfParams.Algorithm.fromInt(algorithm),
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