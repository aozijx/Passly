package com.aozijx.passly.feature.vault.presentation.editor.wifi

import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.policy.WifiQrPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class WifiFormMapperTest {
    @Test
    fun `form maps every wifi and shared field into a typed draft`() {
        val draft = AddWifiFormState(
            ssid = "  Passly  ",
            securityType = WifiSecurityType.Wep,
            password = "abcde",
            isHidden = true,
            notes = "  legacy device  ",
            tags = " work, network, work, , ",
        ).toEntryDraft()

        assertEquals(EntryType.WIFI, draft.type)
        assertEquals(EntryDraftValue.Text("  Passly  "), draft[FieldKey.TITLE])
        assertEquals(EntryDraftValue.Text("  Passly  "), draft[FieldKey.WIFI_SSID])
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
    fun `ssid is the single source for wifi title`() {
        val draft = AddWifiFormState(ssid = "  Office  ").toEntryDraft()

        assertEquals(EntryDraftValue.Text("  Office  "), draft[FieldKey.TITLE])
        assertEquals(EntryDraftValue.Text("  Office  "), draft[FieldKey.WIFI_SSID])
    }

    @Test
    fun `scanned wifi replaces wifi fields but preserves user metadata`() {
        val original = AddWifiFormState(
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

        assertEquals("Guest", updated.ssid)
        assertEquals("", updated.password)
        assertEquals(WifiSecurityType.Open, updated.securityType)
        assertEquals(true, updated.isHidden)
        assertEquals("keep note", updated.notes)
        assertEquals("keep, tags", updated.tags)
    }
}
