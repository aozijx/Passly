package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.policy.EntryEdit
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailDisplayTitleTest {
    @Test
    fun unchangedNetworkNameCancelsEvenWhenProfileTitleDiffers() {
        for (ssid in listOf("Network", " Network ")) {
            assertEquals(DetailTitleEditPlan.Cancel, wifi(ssid).planDetailTitleEdit(ssid))
        }
    }

    @Test
    fun changingNetworkNameToProfileTitleStillProducesNetworkEdit() {
        assertEquals(DetailTitleEditPlan.Save(EntryEdit.SetWifiNetworkName("Personal label")),
            wifi("Old network").planDetailTitleEdit("Personal label"))
    }

    @Test
    fun changedNetworkNamePreservesItsSurroundingWhitespace() {
        assertEquals(DetailTitleEditPlan.Save(EntryEdit.SetWifiNetworkName(" New network ")),
            wifi("Old network").planDetailTitleEdit(" New network "))
    }

    @Test
    fun ordinaryTitleKeepsTrimmingAndBlankCancellation() {
        val login = Entry(EntryIdentity(EntryId("login"), EntryType.LOGIN, timestamps = EntryTimestamps(1)),
            EntryProfile(title = "Mail"), EntrySecret(LoginCredential()))
        assertEquals(DetailTitleEditPlan.Cancel, login.planDetailTitleEdit(" Mail "))
        assertEquals(DetailTitleEditPlan.Cancel, login.planDetailTitleEdit(" "))
        assertEquals(DetailTitleEditPlan.Save(EntryEdit.SetTitle("New title")), login.planDetailTitleEdit(" New title "))
    }

    private fun wifi(ssid: String) = Entry(
        EntryIdentity(EntryId("wifi"), EntryType.WIFI, timestamps = EntryTimestamps(1)),
        EntryProfile(title = "Personal label"), EntrySecret(WifiCredential(ssid)),
    )
}
