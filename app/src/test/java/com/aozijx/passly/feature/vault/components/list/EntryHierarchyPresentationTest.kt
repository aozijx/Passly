package com.aozijx.passly.feature.vault.components.list

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.settings.model.EntryHierarchyDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryHierarchyPresentationTest {

    private val account = item("account", EntryType.ACCOUNT)
    private val login = item("login", EntryType.LOGIN, accountEntryId = account.id)
    private val otp = item("otp", EntryType.OTP, accountEntryId = account.id)
    private val standalone = item("standalone", EntryType.LOGIN)
    private val entries = listOf(login, account, otp, standalone)

    @Test
    fun collapsedShowsAccountAndStandaloneEntry() {
        assertEquals(
            listOf("account", "standalone"),
            arrangeEntryHierarchy(entries, EntryHierarchyDisplayMode.COLLAPSED).map { it.id }
        )
    }

    @Test
    fun expandedPlacesChildrenAfterAccount() {
        assertEquals(
            listOf("account", "login", "otp", "standalone"),
            arrangeEntryHierarchy(entries, EntryHierarchyDisplayMode.EXPANDED).map { it.id }
        )
    }

    @Test
    fun separateHidesAccountContainer() {
        assertEquals(
            listOf("login", "otp", "standalone"),
            arrangeEntryHierarchy(entries, EntryHierarchyDisplayMode.SEPARATE).map { it.id }
        )
    }

    private fun item(
        id: String,
        type: EntryType,
        accountEntryId: String? = null
    ) = EntryListItem(
        id = id,
        entryType = type,
        title = id,
        username = "",
        icon = null,
        iconCustomPath = null,
        website = null,
        favorite = false,
        tags = emptyList(),
        color = null,
        createdAt = 1,
        updatedAt = 1,
        deletedAt = null,
        expiresAt = null,
        lastUsedAt = null,
        usageCount = 0,
        entryVersion = 1,
        capabilityFlags = 0,
        accountEntryId = accountEntryId
    )
}
