package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.codec.entry.payload.CardCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.CustomFieldPayload
import com.aozijx.passly.data.codec.entry.payload.IdentityCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.LoginCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.OtpConfigPayload
import com.aozijx.passly.data.codec.entry.payload.OtpCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.PasskeyCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.data.codec.entry.payload.SshCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.WifiCredentialPayload
import com.aozijx.passly.data.codec.json.AppJson
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.EntryCredentialKind
import com.aozijx.passly.domain.entry.model.credential.IdentityCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.PasskeyCredential
import com.aozijx.passly.domain.entry.model.credential.SshCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EntrySecretMapperTest {
    @Test
    fun `unsupported schema version is rejected before credential mapping`() {
        assertThrows(IllegalArgumentException::class.java) {
            EntrySecretMapper.toDomain(
                SecretPayload(
                    login = LoginCredentialPayload(email = "e@example.test", password = "p"),
                    schemaVersion = 2,
                ),
                EntryType.LOGIN,
            )
        }
    }

    @Test
    fun `otp bundle mapping keeps secret redacted and maps the full secret`() {
        val secret = EntrySecret(OtpCredential(
            OtpConfig(
                type = OtpType.TOTP,
                secret = "otp-secret",
                algorithm = OtpHashAlgorithm.SHA1,
                digits = 6,
                periodSeconds = 30,
                counter = null,
            ),
        ))

        val bundleSecret = secret.toBundleSecret()
        val payload = EntrySecretMapper.toPayload(secret, EntryType.OTP)
        assertEquals(secret, EntrySecretMapper.toDomain(payload, EntryType.OTP))
        assertEquals(null, bundleSecret.otp?.config?.secret)
        assertEquals(null, EntrySecretMapper.toPayload(bundleSecret, EntryType.OTP).otp?.config?.secret)
    }

    @Test
    fun `payload adapter registry rejects duplicate credential kinds`() {
        assertThrows(IllegalArgumentException::class.java) {
            EntryPayloadAdapterRegistry(listOf(OtpEntryPayloadAdapter(), OtpEntryPayloadAdapter()))
        }
    }

    @Test
    fun `every credential kind except none has a registered payload adapter`() {
        EntryCredentialKind.entries
            .filterNot { it == EntryCredentialKind.NONE }
            .forEach { kind -> assertEquals(kind, entryPayloadAdapters.get(kind).kind) }
    }

    @Test
    fun `login adapter is shared by entry types with the same credential kind`() {
        val secret = EntrySecret(LoginCredential(email = "ada@example.test", password = "secret"))
        val adapter = entryPayloadAdapters.get(EntryCredentialKind.LOGIN)

        listOf(EntryType.LOGIN, EntryType.API_KEY).forEach { entryType ->
            val payload = EntrySecretMapper.toPayload(secret, entryType)

            assertEquals(LoginCredentialPayload("ada@example.test", "secret"), payload.login)
            assertEquals(null, payload.otp)
            assertEquals(secret, EntrySecretMapper.toDomain(payload, entryType))
        }
        assertEquals(EntryCredentialKind.LOGIN, adapter.kind)
    }

    @Test
    fun `card adapter owns the full card slot`() {
        val credential = populatedDataClass(CardCredential::class.java) as CardCredential
        val expectedPayload = populatedDataClass(CardCredentialPayload::class.java) as CardCredentialPayload
        val adapter = entryPayloadAdapters.get(EntryCredentialKind.CARD)

        val payload = adapter.encode(credential)

        assertEquals(EntryCredentialKind.CARD, adapter.kind)
        assertEquals(expectedPayload, payload.card)
        assertEquals(null, payload.login)
        assertEquals(credential, adapter.decode(payload))
    }

    @Test
    fun `identity adapter owns the full identity slot`() {
        val credential = populatedDataClass(IdentityCredential::class.java) as IdentityCredential
        val expectedPayload = populatedDataClass(IdentityCredentialPayload::class.java) as IdentityCredentialPayload
        val adapter = entryPayloadAdapters.get(EntryCredentialKind.IDENTITY)

        val payload = adapter.encode(credential)

        assertEquals(EntryCredentialKind.IDENTITY, adapter.kind)
        assertEquals(expectedPayload, payload.identity)
        assertEquals(null, payload.login)
        assertEquals(credential, adapter.decode(payload))
    }

    @Test
    fun `ssh adapter owns the full ssh slot`() {
        val credential = populatedDataClass(SshCredential::class.java) as SshCredential
        val expectedPayload = populatedDataClass(SshCredentialPayload::class.java) as SshCredentialPayload
        val adapter = entryPayloadAdapters.get(EntryCredentialKind.SSH)

        val payload = adapter.encode(credential)

        assertEquals(EntryCredentialKind.SSH, adapter.kind)
        assertEquals(expectedPayload, payload.ssh)
        assertEquals(null, payload.login)
        assertEquals(credential, adapter.decode(payload))
    }

    @Test
    fun `passkey adapter owns the full passkey slot`() {
        val credential = populatedDataClass(PasskeyCredential::class.java) as PasskeyCredential
        val expectedPayload = populatedDataClass(PasskeyCredentialPayload::class.java) as PasskeyCredentialPayload
        val adapter = entryPayloadAdapters.get(EntryCredentialKind.PASSKEY)

        val payload = adapter.encode(credential)

        assertEquals(EntryCredentialKind.PASSKEY, adapter.kind)
        assertEquals(expectedPayload, payload.passkey)
        assertEquals(null, payload.login)
        assertEquals(credential, adapter.decode(payload))
    }

    @Test
    fun `all credential slots retain the fixed legacy json format`() {
        val payload = SecretPayload(
            login = LoginCredentialPayload(email = "e@example.test", password = "p"),
            notes = "note",
            card = CardCredentialPayload(
                cardType = "visa",
                cardNumber = "4111",
                cardExpiry = "12/30",
                cardCvv = "123",
                cardHolder = "Ada",
                paymentPin = "456",
                paymentPlatform = "wallet",
                billingAddress = "address",
                issuingBank = "bank",
                bankBranch = "branch",
                cardProduct = "platinum",
                currency = "CNY",
                bankPhone = "+1",
            ),
            identity = IdentityCredentialPayload(
                idNumber = "id",
                securityQuestion = "question",
                securityAnswer = "answer",
                seedPhrase = "seed words",
                recoveryCodes = listOf("recovery"),
            ),
            ssh = SshCredentialPayload(
                privateKey = "private",
                publicKey = "public",
                passphrase = "phrase",
            ),
            wifi = WifiCredentialPayload(
                ssid = "ssid",
                password = "wifi-pass",
                securityType = "WPA",
                isHidden = true,
            ),
            passkey = PasskeyCredentialPayload(
                credentialId = "credential",
                rpId = "example.test",
                userHandle = "user",
                privateKeyReference = "key-ref",
                hardwareKeyInfo = "hardware",
            ),
            otp = OtpCredentialPayload(
                config = OtpConfigPayload(
                    type = OtpType.HOTP,
                    secret = "otp-secret",
                    algorithm = OtpHashAlgorithm.SHA256,
                    digits = 8,
                    periodSeconds = null,
                    counter = 42,
                    encoding = OtpSecretEncoding.BASE64,
                    issuer = "issuer",
                    accountName = "account",
                ),
            ),
            customFields = listOf(CustomFieldPayload("custom", "value", type = 1)),
        )
        val expectedJson = listOf(
            """{"login":{"email":"e@example.test","password":"p"},"notes":"note","card":{"cardType":"visa","cardNumber":"4111","cardExpiry":"12/30","cardCvv":"123","cardHolder":"Ada",""",
            """ "paymentPin":"456","paymentPlatform":"wallet","billingAddress":"address","issuingBank":"bank","bankBranch":"branch","cardProduct":"platinum","currency":"CNY","bankPhone":"+1"},"identity":{"idNumber":"id","securityQuestion":"question","securityAnswer":"answer","seedPhrase":"seed words","recoveryCodes":["recovery"]},""".trimStart(),
            """ "ssh":{"privateKey":"private","publicKey":"public","passphrase":"phrase"},"wifi":{"ssid":"ssid","password":"wifi-pass","securityType":"WPA","isHidden":true},""".trimStart(),
            """ "passkey":{"credentialId":"credential","rpId":"example.test","userHandle":"user","privateKeyReference":"key-ref","hardwareKeyInfo":"hardware"},""".trimStart(),
            """ "otp":{"config":{"type":"HOTP","secret":"otp-secret","algorithm":"SHA256","digits":8,"periodSeconds":null,"counter":42,"encoding":"BASE64","issuer":"issuer","accountName":"account"}},""".trimStart(),
            """ "customFields":[{"name":"custom","value":"value","type":1}],"schemaVersion":1}""".trimStart(),
        ).joinToString(separator = "")

        val json = AppJson.encodeToString(SecretPayload.serializer(), payload)

        assertEquals(expectedJson, json)
        assertEquals(payload, AppJson.decodeFromString(SecretPayload.serializer(), json))
    }

    @Test
    fun `extended card details survive payload round trip`() {
        val credential = CardCredential(
            cardType = "visa",
            cardNumber = "4111111111111111",
            cardExpiry = "12/30",
            cardCvv = "123",
            cardHolder = "Ada",
            paymentPin = "4567",
            paymentPlatform = "wallet",
            billingAddress = "address",
            issuingBank = "Example Bank",
            bankBranch = "Central Branch",
            cardProduct = "Travel Platinum",
            currency = "CNY / USD",
            bankPhone = "+86 123456",
        )
        val secret = EntrySecret(credential)
        val json = AppJson.encodeToString(
            SecretPayload.serializer(), EntrySecretMapper.toPayload(secret, EntryType.BANK_CARD),
        )
        val restored = EntrySecretMapper.toDomain(
            AppJson.decodeFromString(SecretPayload.serializer(), json), EntryType.BANK_CARD,
        )
        assertEquals(secret, restored)
    }

    @Test
    fun `card domain and payload fields remain structurally aligned`() {
        fun fields(type: Class<*>) = type.declaredFields
            .filterNot {
                it.isSynthetic || it.name == "kind" || java.lang.reflect.Modifier.isStatic(it.modifiers)
            }
            .associate { it.name to it.type }

        assertEquals(fields(CardCredential::class.java), fields(CardCredentialPayload::class.java))
    }

    @Test
    fun `every nondefault card field survives mapper round trip`() {
        val credential = populatedDataClass(CardCredential::class.java) as CardCredential
        val expectedPayload = populatedDataClass(CardCredentialPayload::class.java) as CardCredentialPayload
        val payload = EntrySecretMapper.toPayload(EntrySecret(credential), EntryType.BANK_CARD)

        assertEquals(expectedPayload, payload.card)
        assertEquals(credential, EntrySecretMapper.toDomain(payload, EntryType.BANK_CARD).credential)
    }

    private fun populatedDataClass(type: Class<*>): Any {
        val constructor = type.declaredConstructors
            .filterNot { it.isSynthetic }
            .maxBy { it.parameterCount }
        val arguments = constructor.parameterTypes.mapIndexed { index, parameterType ->
            when {
                parameterType == String::class.java -> "nondefault-$index"
                List::class.java.isAssignableFrom(parameterType) -> listOf("nondefault-$index")
                else -> error("Add a nondefault test value for ${type.simpleName}.${parameterType.simpleName}")
            }
        }
        return constructor.newInstance(*arguments.toTypedArray())
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
            EntryType.WIFI,
        )

        assertEquals("WPA", payload.wifi?.securityType)
    }

    @Test
    fun `wifi adapter round trips the credential slot`() {
        val secret = EntrySecret(WifiCredential(
            ssid = "Office",
            password = "password",
            securityType = WifiSecurityType.WpaPersonal,
            isHidden = true,
        ))
        val adapter = entryPayloadAdapters.get(EntryCredentialKind.WIFI)

        val payload = EntrySecretMapper.toPayload(secret, EntryType.WIFI)

        assertEquals(EntryCredentialKind.WIFI, adapter.kind)
        assertEquals("WPA", payload.wifi?.securityType)
        assertEquals(secret, EntrySecretMapper.toDomain(payload, EntryType.WIFI))
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
                EntryType.WIFI,
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
                EntryType.WIFI,
            )
        }
    }

    @Test
    fun `multiple credential slots are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            EntrySecretMapper.toDomain(
                SecretPayload(
                    login = LoginCredentialPayload(email = "e@example.test", password = "p"),
                    card = CardCredentialPayload(cardNumber = "4111"),
                ),
                EntryType.LOGIN,
            )
        }
    }

    @Test
    fun `credential slot must match the expected entry type`() {
        assertThrows(IllegalArgumentException::class.java) {
            EntrySecretMapper.toDomain(
                SecretPayload(otp = OtpCredentialPayload(OtpConfigPayload(
                    type = OtpType.TOTP,
                    secret = "secret",
                    algorithm = OtpHashAlgorithm.SHA1,
                    digits = 6,
                    periodSeconds = 30,
                    counter = null,
                ))),
                EntryType.LOGIN,
            )
        }
    }

    @Test
    fun `non none entry type requires its credential slot`() {
        assertThrows(IllegalArgumentException::class.java) {
            EntrySecretMapper.toDomain(SecretPayload(), EntryType.LOGIN)
        }
    }

    @Test
    fun `none entry type accepts an empty credential payload`() {
        assertEquals(EntrySecret(), EntrySecretMapper.toDomain(SecretPayload(), EntryType.NOTE))
    }
}
