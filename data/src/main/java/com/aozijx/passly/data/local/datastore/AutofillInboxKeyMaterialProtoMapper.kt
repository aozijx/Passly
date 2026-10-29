package com.aozijx.passly.data.local.datastore

import com.aozijx.passly.data.crypto.proto.AutofillInboxKeyMaterial as AutofillInboxKeyMaterialProto
import com.aozijx.passly.data.crypto.proto.BootstrapData
import com.aozijx.passly.domain.access.model.AutofillInboxKeyMaterial
import com.google.protobuf.ByteString

internal fun BootstrapData.readAutofillInboxKeyMaterial(): AutofillInboxKeyMaterial? =
    if (hasAutofillInboxKey()) autofillInboxKey.toDomain() else null

internal fun BootstrapData.Builder.setAutofillInboxKeyMaterial(
    material: AutofillInboxKeyMaterial,
): BootstrapData.Builder = setAutofillInboxKey(material.toProto())

internal fun BootstrapData.Builder.clearAutofillInboxKeyMaterial(): BootstrapData.Builder =
    clearAutofillInboxKey()

private fun AutofillInboxKeyMaterial.toProto(): AutofillInboxKeyMaterialProto =
    AutofillInboxKeyMaterialProto.newBuilder()
        .setFormatVersion(formatVersion)
        .setKeyId(ByteString.copyFrom(keyId))
        .setPublicKey(ByteString.copyFrom(publicKey))
        .setPublicKeySha256(ByteString.copyFrom(publicKeySha256))
        .setPrivateKeyEnvelope(ByteString.copyFrom(privateKeyEnvelope))
        .build()

private fun AutofillInboxKeyMaterialProto.toDomain(): AutofillInboxKeyMaterial =
    AutofillInboxKeyMaterial(
        formatVersion = formatVersion,
        keyId = keyId.toByteArray(),
        publicKey = publicKey.toByteArray(),
        publicKeySha256 = publicKeySha256.toByteArray(),
        privateKeyEnvelope = privateKeyEnvelope.toByteArray(),
    )
