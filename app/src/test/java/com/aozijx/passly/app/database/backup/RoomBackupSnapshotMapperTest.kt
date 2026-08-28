package com.aozijx.passly.app.database.backup

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.CustomField
import com.aozijx.passly.domain.entry.model.credential.CustomFieldKind
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomBackupSnapshotMapperTest {
    private val mapper = RoomBackupSnapshotMapper()

    @Test
    fun bankCardRoundTripPreservesEveryBusinessField() {
        val entry = Entry(
            identity = EntryIdentity(
                id = EntryId("card-1"),
                type = EntryType.BANK_CARD,
                version = EntryVersion(4),
                timestamps = EntryTimestamps(10L, 20L, 30L),
            ),
            profile = EntryProfile(
                title = "Primary card",
                username = "owner",
                favorite = true,
                tags = setOf("finance"),
                expiresAtMs = 40L,
            ),
            secret = EntrySecret(
                credential = CardCredential(
                    cardType = "credit",
                    cardNumber = "4111111111111111",
                    cardExpiry = "12/30",
                    cardCvv = "123",
                    cardHolder = "Ada Lovelace",
                    paymentPin = "9876",
                    paymentPlatform = "visa",
                    billingAddress = "1 Example Road",
                ),
                notes = "travel",
                customFields = listOf(
                    CustomField("support", "secret", CustomFieldKind.HIDDEN),
                ),
            ),
        )

        assertEquals(entry, mapper.toEntry(mapper.toRecord(entry)))
    }
}
