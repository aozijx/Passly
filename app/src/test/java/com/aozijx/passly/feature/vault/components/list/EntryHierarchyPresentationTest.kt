package com.aozijx.passly.feature.vault.components.list

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.data.settings.model.EntryHierarchyDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryHierarchyPresentationTest {

    private val account = item("account", EntryType.ACCOUNT)
    private val login = item("login", EntryType.LOGIN, accountId = account.id)
    private val otp = item("otp", EntryType.OTP, accountId = account.id)
    private val standalone = item("standalone", EntryType.LOGIN)
    private val entries = listOf(login, account, otp, standalone)

    @Test
    fun collapsedShowsAccountAndStandaloneEntry() {
        assertEquals(
            listOf("account", "standalone"),
            arrangeEntryHierarchy(entries, EntryHierarchyDisplayMode.COLLAPSED).map { it.id.value }
        )
    }

    @Test
    fun expandedPlacesChildrenAfterAccount() {
        assertEquals(
            listOf("account", "login", "otp", "standalone"),
            arrangeEntryHierarchy(entries, EntryHierarchyDisplayMode.EXPANDED).map { it.id.value }
        )
    }

    @Test
    fun separateHidesAccountContainer() {
        assertEquals(
            listOf("login", "otp", "standalone"),
            arrangeEntryHierarchy(entries, EntryHierarchyDisplayMode.SEPARATE).map { it.id.value }
        )
    }

    private fun item(
        id: String,
        type: EntryType,
        accountId: EntryId? = null
    ) = EntryListItem(
        identity = EntryIdentity(
            id = EntryId(id),
            type = type,
            timestamps = EntryTimestamps(1L),
        ),
        profile = EntryProfile(title = id),
        accountId = accountId,
    )
}
