package com.aozijx.passly.presentation.feature.vault.list

import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.query.EntryCapabilities
import com.aozijx.passly.domain.entry.model.query.EntryCapability
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.presentation.ui.vault.list.model.VaultAddTypeUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpKindUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultQuickFilterUiModel
import com.aozijx.passly.presentation.ui.shared.gesture.SwipeActionUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultListUiMapperTest {
    @Test
    fun entryMappingPreservesEveryRenderedValue() {
        val item = EntryListItem(
            identity = EntryIdentity(
                id = EntryId("entry-1"),
                type = EntryType.LOGIN,
                timestamps = EntryTimestamps(1L),
            ),
            profile = EntryProfile(
                title = "Mail",
                username = "user@example.com",
                tags = linkedSetOf("  Personal  "),
                associations = EntryAssociations(primaryUrl = "example.com", applicationIds = setOf("com.example")),
                icon = EntryIcon(customReference = "icons/mail.png"),
                favorite = true,
            ),
            capabilities = EntryCapabilities(setOf(EntryCapability.PASSWORD, EntryCapability.OTP)),
            otpType = OtpType.STEAM,
            otpPreview = "ABC12",
        )

        val ui = item.toUiModel()

        assertEquals("entry-1", ui.id)
        assertEquals("Mail", ui.title)
        assertEquals("user@example.com", ui.username)
        assertEquals("Personal", ui.category)
        assertEquals("example.com", ui.associatedDomain)
        assertEquals("com.example", ui.associatedAppPackage)
        assertEquals(null, ui.iconName)
        assertEquals("icons/mail.png", ui.iconCustomPath)
        assertTrue(ui.favorite)
        assertTrue(ui.hasPassword)
        assertTrue(ui.hasOtp)
        assertEquals(VaultOtpKindUiModel.STEAM, ui.otpKind)
        assertEquals("ABC12", ui.otpPreview)
    }

    @Test
    fun interactionEnumsRoundTripAcrossFeatureBoundary() {
        LibraryQuickFilter.entries.forEach { assertEquals(it, it.toUiModel().toFeatureModel()) }
        SwipeActionType.entries.forEach { assertEquals(it, it.toUiModel().toFeatureModel()) }
        AddType.entries.forEach { assertEquals(it, it.toUiModel().toFeatureModel()) }
        assertEquals(VaultQuickFilterUiModel.ALL, LibraryQuickFilter.ALL.toUiModel())
        assertEquals(SwipeActionUiModel.DELETE, SwipeActionType.DELETE.toUiModel())
        assertEquals(VaultAddTypeUiModel.PASSWORD, AddType.PASSWORD.toUiModel())
    }
}
