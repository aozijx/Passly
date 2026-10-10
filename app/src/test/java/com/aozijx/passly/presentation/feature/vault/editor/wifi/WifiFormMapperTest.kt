package com.aozijx.passly.presentation.feature.vault.editor.wifi

import com.aozijx.passly.domain.entry.model.EntryDraftTarget
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.feature.vault.wifi.WifiQrPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class WifiFormMapperTest {
    @Test
    fun `form maps every wifi and shared field into a typed draft`() {
        val draft = AddWifiFormState(
            title = "  Office network  ",
            titleOrigin = WifiTitleOrigin.USER_EDITED,
            ssid = "  Passly  ",
            securityType = WifiSecurityType.Wep,
            password = "abcde",
            isHidden = true,
            notes = "  legacy device  ",
            tags = " work, network, work, , ",
        ).toEntryDraft()

        assertEquals(EntryDraftTarget.New(EntryType.WIFI), draft.target)
        assertEquals(EntryDraftValue.Text("Office network"), draft[FieldKey.TITLE])
        assertEquals(EntryDraftValue.Text("Passly"), draft[FieldKey.WIFI_SSID])
        assertEquals(EntryDraftValue.Text("WEP"), draft[FieldKey.WIFI_SECURITY])
        assertEquals(EntryDraftValue.Text("abcde"), draft[FieldKey.PASSWORD])
        assertEquals(EntryDraftValue.Toggle(true), draft[FieldKey.WIFI_HIDDEN])
        assertEquals(EntryDraftValue.Text("legacy device"), draft[FieldKey.NOTES])
        assertEquals(
            EntryDraftValue.TextList(listOf("work", "network")),
            draft[FieldKey.TAGS],
        )
    }

    @Test
    fun `switching to open security clears the password`() {
        val form = AddWifiFormState(
            securityType = WifiSecurityType.WpaPersonal,
            password = "password",
        ).withSecurityType(WifiSecurityType.Open)

        assertEquals("", form.password)
        assertEquals(WifiSecurityType.Open, form.securityType)
    }

    @Test
    fun `ssid drives only empty or generated titles`() {
        val generated = AddWifiFormState().withSsid("Office")
        val regenerated = generated.withSsid("Office 2")
        val userEdited = regenerated.withTitle("My network").withSsid("Office 3")

        assertEquals("Office", generated.title)
        assertEquals(WifiTitleOrigin.SSID_GENERATED, generated.titleOrigin)
        assertEquals("Office 2", regenerated.title)
        assertEquals("My network", userEdited.title)
        assertEquals(WifiTitleOrigin.USER_EDITED, userEdited.titleOrigin)
    }

    @Test
    fun `scanned wifi replaces wifi fields but preserves user metadata`() {
        val original = AddWifiFormState(
            title = "My network",
            titleOrigin = WifiTitleOrigin.USER_EDITED,
            ssid = "Old",
            password = "old-password",
            notes = "keep note",
            tags = "keep, tags",
        )

        val updated = original.applyScanned(
            WifiQrPayload(
                ssid = "Guest",
                password = "must-be-cleared",
                securityType = WifiSecurityType.Open,
                isHidden = true,
            ),
        )

        assertEquals("My network", updated.title)
        assertEquals("Guest", updated.ssid)
        assertEquals("", updated.password)
        assertEquals(WifiSecurityType.Open, updated.securityType)
        assertEquals(true, updated.isHidden)
        assertEquals("keep note", updated.notes)
        assertEquals("keep, tags", updated.tags)
    }
}
