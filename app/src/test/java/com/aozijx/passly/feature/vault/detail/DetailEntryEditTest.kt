package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.SshCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailEntryEditTest {

    @Test
    fun titleEditPreservesLatestUnrelatedFields() {
        val latest = loginEntry()

        val actual = DetailEntryEdit.SetTitle("Renamed").applyRequired(latest)

        assertEquals("Renamed", actual.title)
        assertEquals(latest.copy(profile = latest.profile.copy(title = "Renamed")), actual)
    }

    @Test
    fun favoriteEditPreservesLatestUnrelatedFields() {
        val latest = loginEntry()

        val actual = DetailEntryEdit.ToggleFavorite.applyRequired(latest)

        assertEquals(false, actual.favorite)
        assertEquals(latest.copy(profile = latest.profile.copy(favorite = false)), actual)
    }

    @Test
    fun usernameEditPreservesLatestUnrelatedFields() {
        val latest = loginEntry()

        val actual = DetailEntryEdit.SetSensitiveField(
            FieldKey.USERNAME,
            "latest-user",
        ).applyRequired(latest)

        assertEquals("latest-user", actual.username)
        assertEquals(latest.copy(profile = latest.profile.copy(username = "latest-user")), actual)
    }

    @Test
    fun loginPasswordEditPreservesLatestCredentialFields() {
        val latest = loginEntry()

        val actual = DetailEntryEdit.SetSensitiveField(
            FieldKey.PASSWORD,
            "new-password",
        ).applyRequired(latest)

        assertEquals("new-password", actual.secret.login?.password)
        assertEquals("mail@example.com", actual.secret.login?.email)
        assertEquals(latest.profile, actual.profile)
    }

    @Test
    fun cardEditsPreserveOtherCardFields() {
        val latest = cardEntry()

        val withNumber = DetailEntryEdit.SetSensitiveField(
            FieldKey.CARD_NUMBER,
            "5555555555554444",
        ).applyRequired(latest)
        val withCvv = DetailEntryEdit.SetSensitiveField(
            FieldKey.CARD_CVV,
            "999",
        ).applyRequired(latest)

        assertEquals("5555555555554444", withNumber.secret.card?.cardNumber)
        assertEquals("123", withNumber.secret.card?.cardCvv)
        assertEquals("4111111111111111", withCvv.secret.card?.cardNumber)
        assertEquals("999", withCvv.secret.card?.cardCvv)
        assertEquals(latest.profile, withNumber.profile)
        assertEquals(latest.profile, withCvv.profile)
    }

    @Test
    fun wifiPasswordEditPreservesWifiMetadata() {
        val latest = wifiEntry()

        val actual = DetailEntryEdit.SetSensitiveField(
            FieldKey.PASSWORD,
            "new-wifi-password",
        ).applyRequired(latest)

        assertEquals("new-wifi-password", actual.secret.wifi?.password)
        assertEquals("WPA3", actual.secret.wifi?.securityType)
        assertEquals(true, actual.secret.wifi?.isHidden)
        assertEquals(latest.profile, actual.profile)
    }

    @Test
    fun sshPassphraseEditPreservesKeys() {
        val latest = sshEntry()

        val actual = DetailEntryEdit.SetSensitiveField(
            FieldKey.SSH_PASSPHRASE,
            "new-passphrase",
        ).applyRequired(latest)

        assertEquals("new-passphrase", actual.secret.ssh?.passphrase)
        assertEquals("private", actual.secret.ssh?.privateKey)
        assertEquals("public", actual.secret.ssh?.publicKey)
        assertEquals(latest.profile, actual.profile)
    }

    @Test
    fun notesEditPreservesCredentialAndProfile() {
        val latest = loginEntry()

        val actual = DetailEntryEdit.SetNotes("new notes").applyRequired(latest)

        assertEquals("new notes", actual.secret.notes)
        assertEquals(latest.secret.credential, actual.secret.credential)
        assertEquals(latest.profile, actual.profile)
    }

    @Test
    fun associationEditsPreserveMatchDomains() {
        val latest = loginEntry()

        val withUrl = DetailEntryEdit.SetPrimaryUrl(
            "https://new.example.com",
        ).applyRequired(latest)
        val actual = DetailEntryEdit.SetApplicationIds(
            setOf("com.example.new"),
        ).applyRequired(withUrl)

        assertEquals("https://new.example.com", actual.associations.primaryUrl)
        assertEquals(setOf("com.example.new"), actual.associations.applicationIds)
        assertEquals(setOf("example.com", "login.example.com"), actual.associations.domains)
        assertEquals(latest.icon, actual.icon)
        assertEquals(latest.secret, actual.secret)
    }

    @Test
    fun tagsEditPreservesIconAndAssociations() {
        val latest = loginEntry()

        val actual = DetailEntryEdit.SetTags(linkedSetOf("Finance", "Work")).applyRequired(latest)

        assertEquals(linkedSetOf("Finance", "Work"), actual.tags)
        assertEquals(latest.icon, actual.icon)
        assertEquals(latest.associations, actual.associations)
        assertEquals(latest.secret, actual.secret)
    }

    @Test
    fun iconEditPreservesDomainAndTags() {
        val latest = loginEntry()
        val icon = EntryIcon(name = "security.key", color = "primary")

        val actual = DetailEntryEdit.SetIcon(icon).applyRequired(latest)

        assertEquals(icon, actual.icon)
        assertEquals(latest.associations, actual.associations)
        assertEquals(latest.tags, actual.tags)
        assertEquals(latest.secret, actual.secret)
    }

    @Test
    fun unsupportedSensitiveFieldDoesNotProduceAnEntryUpdate() {
        val actual = DetailEntryEdit.SetSensitiveField(
            FieldKey.PAYMENT_PIN,
            "1234",
        ).applyTo(cardEntry())

        assertNull(actual)
    }

    private fun loginEntry() = entry(
        type = EntryType.LOGIN,
        secret = EntrySecret(
            credential = LoginCredential(
                email = "mail@example.com",
                password = "old-password",
            ),
            notes = "latest notes",
        ),
    )

    private fun cardEntry() = entry(
        type = EntryType.BANK_CARD,
        secret = EntrySecret(
            credential = CardCredential(
                cardNumber = "4111111111111111",
                cardCvv = "123",
                cardExpiry = "12/30",
                cardHolder = "Latest Holder",
            ),
            notes = "latest notes",
        ),
    )

    private fun wifiEntry() = entry(
        type = EntryType.WIFI,
        secret = EntrySecret(
            credential = WifiCredential(
                ssid = "Latest Network",
                password = "old-wifi-password",
                securityType = "WPA3",
                isHidden = true,
            ),
            notes = "latest notes",
        ),
    )

    private fun sshEntry() = entry(
        type = EntryType.SSH_KEY,
        secret = EntrySecret(
            credential = SshCredential(
                privateKey = "private",
                publicKey = "public",
                passphrase = "old-passphrase",
            ),
            notes = "latest notes",
        ),
    )

    private fun entry(type: EntryType, secret: EntrySecret) = Entry(
        identity = EntryIdentity(
            id = EntryId("entry-1"),
            type = type,
            version = EntryVersion(7),
            timestamps = EntryTimestamps(createdAtMs = 10, updatedAtMs = 20),
        ),
        profile = EntryProfile(
            title = "Latest title",
            username = "latest-user",
            associations = EntryAssociations(
                primaryUrl = "https://login.example.com",
                domains = setOf("example.com", "login.example.com"),
                applicationIds = setOf("com.example.latest"),
            ),
            icon = EntryIcon(
                name = "legacy-icon",
                customReference = "/private/favicon.webp",
                color = "secondary",
            ),
            favorite = true,
            tags = linkedSetOf("Personal", "Latest"),
            expiresAtMs = 9999,
        ),
        secret = secret,
    )

    private fun DetailEntryEdit.applyRequired(entry: Entry): Entry =
        requireNotNull(applyTo(entry))
}
