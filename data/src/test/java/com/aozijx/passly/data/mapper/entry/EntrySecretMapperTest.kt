package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.data.codec.entry.payload.WifiCredentialPayload
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EntrySecretMapperTest {
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
