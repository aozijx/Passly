package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.CustomField
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryFormEditorTest {
    private val editor = EntryFormEditor()

    @Test
    fun appliesLoginFormAndPreservesUnownedProfileAndCustomFields() {
        val original = entry(
            EntryType.LOGIN,
            LoginCredential(password = "old"),
        )

        val updated = apply(original, draft(
            EntryType.LOGIN,
            FieldKey.TITLE to EntryDraftValue.Text("New title"),
            FieldKey.USERNAME to EntryDraftValue.Text("new-user"),
            FieldKey.PASSWORD to EntryDraftValue.Text("new-secret"),
            FieldKey.PRIMARY_URL to EntryDraftValue.Text("https://new.example"),
            FieldKey.TAGS to EntryDraftValue.TextList(listOf("new")),
        ))

        assertEquals("New title", updated.title)
        assertEquals("new-user", updated.username)
        assertEquals("new-secret", updated.secret.login?.password)
        assertEquals("https://new.example", updated.associations.primaryUrl)
        assertEquals(setOf("new"), updated.tags)
        assertPreservedState(original, updated)
        assertEquals(original.associations.domains, updated.associations.domains)
        assertTrue(updated.associations.applicationIds.isEmpty())
    }

    @Test
    fun appliesOtpCardAndWifiForms() {
        val otp = apply(
            entry(EntryType.OTP, OtpCredential(OtpConfig(type = OtpType.TOTP, secret = "JBSWY3DPEHPK3PXP"))),
            draft(EntryType.OTP,
                FieldKey.TITLE to EntryDraftValue.Text("Authenticator"),
                FieldKey.OTP_SECRET to EntryDraftValue.Text("MFRGGZDFMZTWQ2LK"),
                FieldKey.TAGS to EntryDraftValue.TextList(listOf("otp")),
            ),
        )
        val card = apply(
            entry(EntryType.BANK_CARD, CardCredential(cardNumber = "4111111111111111", cardCvv = "123")),
            draft(EntryType.BANK_CARD,
                FieldKey.TITLE to EntryDraftValue.Text("New Card"),
                FieldKey.CARD_NUMBER to EntryDraftValue.Text("5555555555554444"),
                FieldKey.CARD_CVV to EntryDraftValue.Text("456"),
            ),
        )
        val wifi = apply(
            entry(EntryType.WIFI, WifiCredential("Old Wi-Fi", "old-pass")),
            draft(EntryType.WIFI,
                FieldKey.TITLE to EntryDraftValue.Text("New Wi-Fi"),
                FieldKey.WIFI_SSID to EntryDraftValue.Text("New Wi-Fi"),
                FieldKey.PASSWORD to EntryDraftValue.Text("new-pass"),
                FieldKey.WIFI_SECURITY to EntryDraftValue.Text(WifiSecurityType.WpaPersonal.storedValue),
                FieldKey.WIFI_HIDDEN to EntryDraftValue.Toggle(true),
            ),
        )

        assertEquals("MFRGGZDFMZTWQ2LK", otp.secret.otp?.config?.secret)
        assertEquals("5555555555554444", card.secret.card?.cardNumber)
        assertEquals("456", card.secret.card?.cardCvv)
        assertEquals("New Wi-Fi", wifi.secret.wifi?.ssid)
        assertEquals("new-pass", wifi.secret.wifi?.password)
        assertTrue(wifi.secret.wifi?.isHidden == true)
        assertEquals(setOf("kept.example"), otp.associations.domains)
    }

    @Test
    fun rejectsTypeMismatchUnsupportedTypeAndInvalidCredential() {
        val login = entry(EntryType.LOGIN, LoginCredential(password = "old"))
        assertEquals(EntryEditResult.Unsupported, editor.apply(login, draft(EntryType.WIFI)))
        assertEquals(
            EntryEditResult.Unsupported,
            editor.apply(entry(EntryType.NOTE, com.aozijx.passly.domain.entry.model.credential.EntryCredential.None), draft(EntryType.NOTE)),
        )

        val invalid = editor.apply(
            login,
            draft(EntryType.LOGIN,
                FieldKey.TITLE to EntryDraftValue.Text("Login"),
                FieldKey.PASSWORD to EntryDraftValue.Text(""),
            ),
        ) as EntryEditResult.Invalid
        assertEquals(setOf(FieldKey.PASSWORD), invalid.violations.mapTo(linkedSetOf()) { it.field })
    }

    private fun apply(entry: Entry, draft: EntryDraft): Entry =
        (editor.apply(entry, draft) as EntryEditResult.Updated).entry

    private fun draft(type: EntryType, vararg fields: Pair<FieldKey, EntryDraftValue>): EntryDraft {
        val definition = EntryTypeDefinitions[type]
        return fields.fold(EntryDraft(type)) { current, (key, value) -> current.withValue(definition, key, value) }
    }

    private fun entry(type: EntryType, credential: com.aozijx.passly.domain.entry.model.credential.EntryCredential) = Entry(
        identity = EntryIdentity(
            id = com.aozijx.passly.domain.entry.model.EntryId("entry-1"),
            type = type,
            version = EntryVersion(4),
            timestamps = EntryTimestamps(10, 20),
        ),
        profile = EntryProfile(
            title = "Old title",
            username = "kept-user",
            associations = EntryAssociations(
                primaryUrl = "https://kept.example",
                domains = setOf("kept.example"),
                applicationIds = setOf("com.kept.app"),
            ),
            icon = EntryIcon(name = "kept-icon", customReference = "private-icon"),
            favorite = true,
            tags = setOf("old"),
            expiresAtMs = 900,
        ),
        secret = EntrySecret(credential = credential, notes = "old notes", customFields = listOf(CustomField("custom", "retained"))),
    )

    private fun assertPreservedState(before: Entry, after: Entry) {
        assertEquals(before.identity, after.identity)
        assertEquals(before.favorite, after.favorite)
        assertEquals(before.icon, after.icon)
        assertEquals(before.profile.expiresAtMs, after.profile.expiresAtMs)
        assertEquals(before.secret.customFields, after.secret.customFields)
    }
}
