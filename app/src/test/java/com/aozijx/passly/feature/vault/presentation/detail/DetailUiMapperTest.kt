package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.feature.vault.presentation.detail.section.DetailSectionKey
import com.aozijx.passly.domain.entry.model.activity.EntryActivityContext
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailOtpUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailFieldUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailSectionUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailUiMapperTest {
    @Test
    fun bankCardDoesNotUseProfileUsernameAsCardholder() {
        val entry = Entry(
            identity = EntryIdentity(id = EntryId("card"), type = EntryType.BANK_CARD, timestamps = EntryTimestamps(1L)),
            profile = EntryProfile(title = "Card", username = "profile-user"),
            secret = EntrySecret(credential = CardCredential()),
        )
        val state = DetailUiState(entry = entry, sections = listOf(DetailSectionKey.BANK_CARD))
        val card = requireNotNull(toDetailPresentationModel(state, null)?.content?.bankCard)

        assertNull(card.cardholder)
    }
    @Test
    fun headerMapperKeepsOnlyHeaderState() {
        val entry = noteEntry()
        val state = DetailUiState(
            entry = entry,
            editedTitle = "Edited",
            isEditingTitle = true,
        )

        val ui = detailHeaderUiModel(entry, state)

        assertEquals("Example", ui.title)
        assertTrue(ui.favorite)
        assertEquals("Edited", ui.editedTitle)
        assertTrue(ui.isEditingTitle)
    }

    @Test
    fun contentMapperProjectsOnlySharedContentModels() {
        val entry = noteEntry()
        val state = DetailUiState(
            entry = entry,
            relatedEntries = listOf(entry.copy(identity = entry.identity.copy(id = EntryId("related")))),
            history = listOf(
                EntryActivity(
                    entryId = entry.id.value,
                    activityType = ActivityType.AUTOFILL,
                    createdAt = 3L,
                )
            ),
        )

        val ui = detailContentUiModel(entry, state)

        assertEquals("related", ui.relatedEntries.single().id)
        assertEquals(1L, ui.metadata.createdAt)
        assertEquals(1L, ui.metadata.updatedAt)
        assertEquals(ActivityType.AUTOFILL, ui.activities.single().type)
        assertEquals(EntryActivityContext.Usage, ui.activities.single().context)
        assertEquals(3L, ui.activities.single().createdAt)
    }

    @Test
    fun otpMapperPreservesVolatileOtpState() {
        val ui = detailOtpUiModel(
            OtpCodeState.Ready(
                code = "123456",
                progress = 0.25f,
            )
        )

        require(ui is DetailOtpUiModel.Ready)
        assertEquals("123456", ui.code)
        assertEquals(0.25f, ui.progress)
    }

    @Test
    fun scopedSensitiveTextRedactsToStringAndWipesTemporaryCopy() {
        val source = OwnedChars.fromString("secret-value")
        val scoped = source.asScopedSensitiveText()
        lateinit var borrowed: CharArray

        val length = scoped.useChars {
            borrowed = it
            assertEquals("secret-value", String(it))
            it.size
        }

        assertEquals(12, length)
        assertEquals("***", scoped.toString())
        assertTrue(borrowed.all { it == '\u0000' })
        assertFalse(source.isWiped)
        source.close()
    }

    @Test
    fun presentationMapperProjectsEditorOwnershipAndSaveProgress() {
        val entry = noteEntry()
        val state = DetailUiState(
            entry = entry,
            fieldEdits = DetailFieldEditState().start(DetailDraftKey.Notes, "draft"),
            saveState = DetailSaveState.Saving(DetailEditCompletion.Tags),
        )

        val presentation = requireNotNull(
            toDetailPresentationModel(state, null),
        )

        assertTrue(presentation.content.notes.isEditing)
        assertEquals("draft", presentation.content.notes.editedNotes)
        assertTrue(presentation.overlays.savingTags)
        assertFalse(presentation.overlays.savingIcon)
    }

    @Test
    fun presentationMapperProjectsOnlyRenderableSections() {
        val state = DetailUiState(
            entry = noteEntry(),
            sections = listOf(
                DetailSectionKey.CREDENTIAL,
                DetailSectionKey.ENTRY_TYPE,
                DetailSectionKey.ASSOCIATED_INFO,
                DetailSectionKey.NOTES,
                DetailSectionKey.METADATA,
                DetailSectionKey.ACTIVITY,
            ),
        )

        val presentation = requireNotNull(toDetailPresentationModel(state, null))

        assertEquals(setOf(DetailSectionUiModel.CREDENTIAL), presentation.content.sections)
    }

    @Test
    fun `wifi presentation uses ssid as common title and common password field`() {
        val entry = Entry(
            identity = EntryIdentity(
                id = EntryId("wifi"),
                type = EntryType.WIFI,
                timestamps = EntryTimestamps(1L),
            ),
            profile = EntryProfile(title = "Legacy title"),
            secret = EntrySecret(
                credential = WifiCredential(ssid = "Office", password = "wifi-secret"),
            ),
        )
        val state = DetailUiState(
            entry = entry,
            sections = listOf(DetailSectionKey.CREDENTIAL),
        )

        val presentation = requireNotNull(toDetailPresentationModel(state, null))

        assertEquals("Office", presentation.header.title)
        assertTrue(presentation.content.canShowWifiQr)
        assertFalse(requireNotNull(presentation.content.credential).username.visible)
        assertTrue(requireNotNull(presentation.content.credential).password.visible)
    }

    @Test
    fun `bank card presentation keeps stored sensitive fields visible while locked`() {
        val entry = Entry(
            identity = EntryIdentity(
                id = EntryId("bank-card"),
                type = EntryType.BANK_CARD,
                timestamps = EntryTimestamps(1L),
            ),
            profile = EntryProfile(title = "Daily card"),
            secret = EntrySecret(
                credential = CardCredential(
                    cardHolder = "Alice",
                    cardExpiry = "08/2030",
                    issuingBank = "Bank", bankBranch = "Branch", cardProduct = "Product",
                    currency = "CNY / USD", bankPhone = "+86 123456",
                ),
            ),
        )
        val state = DetailUiState(
            entry = entry,
            sections = listOf(DetailSectionKey.BANK_CARD),
            sensitiveFieldKeys = setOf(
                SensitiveFieldKey.CARD_NUMBER,
                SensitiveFieldKey.CARD_CVV,
                SensitiveFieldKey.CARD_PAYMENT_PIN,
            ),
        )

        val card = requireNotNull(toDetailPresentationModel(state, null)?.content?.bankCard)

        assertEquals("Alice", card.cardholder)
        val bankDetails = card.bankDetails.associate { it.field to it.value }
        assertEquals("Bank", bankDetails[DetailFieldUiModel.CARD_ISSUING_BANK])
        assertEquals("Branch", bankDetails[DetailFieldUiModel.CARD_BANK_BRANCH])
        assertEquals("Product", bankDetails[DetailFieldUiModel.CARD_PRODUCT])
        assertEquals("CNY / USD", bankDetails[DetailFieldUiModel.CARD_CURRENCY])
        assertEquals("+86 123456", bankDetails[DetailFieldUiModel.CARD_BANK_PHONE])
        assertTrue(card.cardholderRevealed)
        assertTrue(card.hasCardNumber)
        assertTrue(card.hasCvv)
        assertTrue(card.hasPaymentPin)
        assertTrue(card.canRevealMore)
    }

    private fun noteEntry() = Entry(
        identity = EntryIdentity(
            id = EntryId("entry"),
            type = EntryType.NOTE,
            timestamps = EntryTimestamps(1L),
        ),
        profile = EntryProfile(title = "Example", username = "user", favorite = true),
        secret = EntrySecret(notes = "note"),
    )
}
