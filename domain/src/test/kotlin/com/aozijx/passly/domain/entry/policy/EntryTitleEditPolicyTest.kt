package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryTitleEditPolicyTest {
    @Test
    fun titleEditValueUsesWifiNetworkNameAndProfileTitleForOtherTypes() {
        assertEquals("Network", wifi("Network").titleEditValue)
        assertEquals("Mail", login().titleEditValue)
    }

    @Test
    fun unchangedNetworkNameIsNotAnEditEvenWhenProfileTitleDiffers() {
        for (ssid in listOf("Network", " Network ")) {
            assertEquals(EntryTitleEditDecision.Unchanged, wifi(ssid).planTitleEdit(ssid))
        }
    }

    @Test
    fun networkNameEditPreservesWhitespaceAndCanEqualTheProfileTitle() {
        assertEquals(
            EntryTitleEditDecision.Change(EntryEdit.SetWifiNetworkName("Personal label")),
            wifi("Old network").planTitleEdit("Personal label"),
        )
        assertEquals(
            EntryTitleEditDecision.Change(EntryEdit.SetWifiNetworkName(" New network ")),
            wifi("Old network").planTitleEdit(" New network "),
        )
    }

    @Test
    fun ordinaryTitleTrimsAndRejectsBlankOrUnchangedValues() {
        val login = login()

        assertEquals(EntryTitleEditDecision.Unchanged, login.planTitleEdit(" Mail "))
        assertEquals(EntryTitleEditDecision.Unchanged, login.planTitleEdit(" "))
        assertEquals(
            EntryTitleEditDecision.Change(EntryEdit.SetTitle("New title")),
            login.planTitleEdit(" New title "),
        )
    }

    private fun wifi(ssid: String) =
        Entry(
            EntryIdentity(EntryId("wifi"), EntryType.WIFI, timestamps = EntryTimestamps(1)),
            EntryProfile(title = "Personal label"),
            EntrySecret(WifiCredential(ssid)),
        )

    private fun login() =
        Entry(
            EntryIdentity(EntryId("login"), EntryType.LOGIN, timestamps = EntryTimestamps(1)),
            EntryProfile(title = "Mail"),
            EntrySecret(LoginCredential()),
        )
}
