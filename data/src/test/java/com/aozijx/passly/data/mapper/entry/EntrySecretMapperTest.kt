package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.data.codec.json.AppJson
import com.aozijx.passly.data.codec.entry.payload.WifiCredentialPayload
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EntrySecretMapperTest {
    @Test
    fun `extended card details survive payload round trip`() {
        val secret = EntrySecret(CardCredential(
            cardHolder = "Ada", issuingBank = "Example Bank", bankBranch = "Central Branch",
            cardProduct = "Travel Platinum", currency = "CNY / USD", bankPhone = "+86 123456",
        ))
        val json = AppJson.encodeToString(SecretPayload.serializer(), EntrySecretMapper.toPayload(secret))
        val restored = EntrySecretMapper.toDomain(AppJson.decodeFromString(SecretPayload.serializer(), json))
        assertEquals(secret, restored)
    }

    @Test
    fun `known wifi security is stored as a canonical string`() {
        val payload = EntrySecretMapper.toPayload(
            EntrySecret(
                WifiCredential(
                    ssid = "Office",
                    password = "password",
                    securityType = WifiSecurityType.WpaPersonal,
                ),
            ),
        )

        assertEquals("WPA", payload.wifi?.securityType)
    }

    @Test
    fun `missing stored wifi security is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            EntrySecretMapper.toDomain(
                SecretPayload(
                    wifi = WifiCredentialPayload(
                        ssid = "Legacy",
                        password = "password",
                        securityType = null,
                    ),
                ),
            )
        }
    }

    @Test
    fun `unsupported stored wifi security is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            EntrySecretMapper.toDomain(
                SecretPayload(
                    wifi = WifiCredentialPayload(
                        ssid = "Lab",
                        password = "password",
                        securityType = "vendor-private",
                    ),
                ),
            )
        }
    }
}
