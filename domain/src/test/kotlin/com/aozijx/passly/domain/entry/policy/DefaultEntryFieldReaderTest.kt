package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.PasskeyCredential
import com.aozijx.passly.domain.entry.model.credential.SshCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultEntryFieldReaderTest {
    private val reader = DefaultEntryFieldReader()
    private val entry = Entry(
        identity = EntryIdentity(
            id = EntryId("entry-id"),
            type = EntryType.ACCOUNT,
            timestamps = EntryTimestamps(createdAtMs = 1L),
        ),
        profile = EntryProfile(
            title = "Example",
            username = "display-name",
            associations = EntryAssociations(
                primaryUrl = "https://example.com/login",
                domains = linkedSetOf("example.com", "example.org"),
            ),
        ),
    )

    @Test
    fun wifiSecurityIsReadFromWifiCredential() {
        val wifi = entry.copy(identity = entry.identity.copy(type = EntryType.WIFI), secret = EntrySecret(
            WifiCredential(ssid = "Network", securityType = WifiSecurityType.Open),
        ))
        assertEquals(WifiSecurityType.Open.storedValue, reader.getFieldValue(wifi, FieldKey.WIFI_SECURITY))
    }

    @Test
    fun unrelatedCredentialsDoNotInventOtpConfiguration() {
        for (key in listOf(FieldKey.OTP_PERIOD, FieldKey.OTP_DIGITS, FieldKey.OTP_ALGORITHM)) {
            assertNull("Absent OTP field $key", reader.getFieldValue(entry, key))
        }
        assertNull(reader.getFieldValue(entry, FieldKey.WIFI_HIDDEN))
    }

    @Test
    fun hotpPreservesAbsentPeriodAndUnrevealedSecret() {
        val otp = entry.copy(identity = entry.identity.copy(type = EntryType.OTP), secret = EntrySecret(OtpCredential(
            OtpConfig(type = OtpType.HOTP, counter = 17, periodSeconds = null,
                digits = 8, algorithm = OtpHashAlgorithm.SHA512, encoding = OtpSecretEncoding.BASE64),
        )))
        assertNull(reader.getFieldValue(otp, FieldKey.OTP_PERIOD))
        assertNull(reader.getFieldValue(otp, FieldKey.OTP_SECRET))
        assertEquals("17", reader.getFieldValue(otp, FieldKey.OTP_COUNTER))
        assertEquals("8", reader.getFieldValue(otp, FieldKey.OTP_DIGITS))
        assertEquals("HOTP", reader.getFieldValue(otp, FieldKey.OTP_TYPE))
        assertEquals("SHA512", reader.getFieldValue(otp, FieldKey.OTP_ALGORITHM))
        assertEquals("BASE64", reader.getFieldValue(otp, FieldKey.OTP_ENCODING))
    }

    @Test
    fun readsCardNumberAndTypeWithoutSubstitutingOtherFields() {
        val card = entry.copy(identity = entry.identity.copy(type = EntryType.BANK_CARD), secret = EntrySecret(
            CardCredential(cardType = "debit", cardNumber = "4111111111111111"),
        ))
        assertEquals("debit", reader.getFieldValue(card, FieldKey.CARD_TYPE))
        assertEquals("4111111111111111", reader.getFieldValue(card, FieldKey.CARD_NUMBER))
        assertNull(reader.getFieldValue(card, FieldKey.CARD_CVV))
    }

    @Test
    fun sshPublicKeyAndPassphraseRemainDistinct() {
        val ssh = entry.copy(identity = entry.identity.copy(type = EntryType.SSH_KEY), secret = EntrySecret(
            SshCredential(privateKey = null, publicKey = "ssh-ed25519 public", passphrase = "phrase"),
        ))
        assertEquals("ssh-ed25519 public", reader.getFieldValue(ssh, FieldKey.SSH_PUBLIC_KEY))
        assertEquals("phrase", reader.getFieldValue(ssh, FieldKey.SSH_PASSPHRASE))
        assertNull(reader.getFieldValue(ssh, FieldKey.SSH_KEY))
    }

    @Test
    fun readsPasskeyMetadataWithoutPrivateReference() {
        val passkey = entry.copy(identity = entry.identity.copy(type = EntryType.PASSKEY), secret = EntrySecret(
            PasskeyCredential(credentialId = "credential", rpId = "example.com", userHandle = "handle"),
        ))
        assertEquals("credential", reader.getFieldValue(passkey, FieldKey.PASSKEY_CREDENTIAL_ID))
        assertEquals("example.com", reader.getFieldValue(passkey, FieldKey.PASSKEY_RELYING_PARTY_ID))
        assertEquals("handle", reader.getFieldValue(passkey, FieldKey.PASSKEY_USER_HANDLE))
        assertNull(reader.getFieldValue(passkey, FieldKey.PASSKEY_DATA))
    }

    @Test
    fun scalarMetadataUsesRawValuesAndLeavesCustomCollectionUnserialized() {
        val metadata = entry.copy(profile = entry.profile.copy(
            tags = linkedSetOf("one", "two"), favorite = true, expiresAtMs = 42L,
            associations = entry.profile.associations.copy(applicationIds = linkedSetOf("app.one", "app.two")),
        ))
        assertEquals("one, two", reader.getFieldValue(metadata, FieldKey.TAGS))
        assertEquals("true", reader.getFieldValue(metadata, FieldKey.FAVORITE))
        assertEquals("42", reader.getFieldValue(metadata, FieldKey.EXPIRES_AT))
        assertEquals("app.one, app.two", reader.getFieldValue(metadata, FieldKey.APPLICATION_IDS))
        assertNull(reader.getFieldValue(metadata, FieldKey.CUSTOM_FIELDS))
    }

    @Test
    fun profileUsernameDoesNotBecomeCredentialFields() {
        assertEquals("display-name", reader.getFieldValue(entry, FieldKey.USERNAME))
        assertNull(reader.getFieldValue(entry, FieldKey.CARD_HOLDER))
        assertNull(reader.getFieldValue(entry, FieldKey.WIFI_SSID))
    }

    @Test
    fun readsCardDetailsFromTheirDedicatedCredentialFields() {
        val card = entry.copy(identity = entry.identity.copy(type = EntryType.BANK_CARD), secret = EntrySecret(CardCredential(
            cardHolder = "Ada", issuingBank = "Bank", bankBranch = "Branch", cardProduct = "Product",
            currency = "CNY / USD", bankPhone = "+86 123456", billingAddress = "Address",
        )))
        for ((key, value) in mapOf(
            FieldKey.CARD_HOLDER to "Ada", FieldKey.CARD_ISSUING_BANK to "Bank", FieldKey.CARD_BANK_BRANCH to "Branch",
            FieldKey.CARD_PRODUCT to "Product", FieldKey.CARD_CURRENCY to "CNY / USD", FieldKey.CARD_BANK_PHONE to "+86 123456",
            FieldKey.BILLING_ADDRESS to "Address",
        )) assertEquals(value, reader.getFieldValue(card, key))
    }

    @Test
    fun readsSsidFromWifiCredentialRatherThanProfileUsername() {
        val wifi = entry.copy(identity = entry.identity.copy(type = EntryType.WIFI), secret = EntrySecret(
            WifiCredential(ssid = "Actual Network", securityType = WifiSecurityType.Open),
        ))
        assertEquals("Actual Network", reader.getFieldValue(wifi, FieldKey.WIFI_SSID))
        assertEquals("false", reader.getFieldValue(wifi, FieldKey.WIFI_HIDDEN))
    }
    @Test
    fun readsPrimaryUrlFromItsDedicatedFieldKey() {
        assertEquals(
            "https://example.com/login",
            reader.getFieldValue(entry, FieldKey.PRIMARY_URL),
        )
    }

    @Test
    fun readsDomainsFromTheirDedicatedFieldKey() {
        assertEquals(
            "example.com, example.org",
            reader.getFieldValue(entry, FieldKey.DOMAINS),
        )
    }
}
