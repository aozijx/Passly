package com.aozijx.passly.app.entry.policy

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
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
