package com.aozijx.passly.feature.vault.presentation.navigation

import com.aozijx.passly.app.navigation.AppRoute
import com.aozijx.passly.domain.entry.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VaultEditorDestinationRegistryTest {
    @Test
    fun editRoutesExistOnlyForTheFourSupportedCreationTypes() {
        assertEquals(
            AppRoute.EditPassword.createRoute("id"),
            VaultEditorDestinationRegistry.editRouteFor(EntryType.LOGIN, "id"),
        )
        assertEquals(
            AppRoute.EditOtp.createRoute("id"),
            VaultEditorDestinationRegistry.editRouteFor(EntryType.OTP, "id"),
        )
        assertEquals(
            AppRoute.EditBankCard.createRoute("id"),
            VaultEditorDestinationRegistry.editRouteFor(EntryType.BANK_CARD, "id"),
        )
        assertEquals(
            AppRoute.EditWifi.createRoute("id"),
            VaultEditorDestinationRegistry.editRouteFor(EntryType.WIFI, "id"),
        )
        EntryType.entries
            .filterNot { it in setOf(EntryType.LOGIN, EntryType.OTP, EntryType.BANK_CARD, EntryType.WIFI) }
            .forEach { assertNull(VaultEditorDestinationRegistry.editRouteFor(it, "id")) }
    }

    @Test
    fun fullFormEditCapabilityMatchesTheRouteRegistryForEveryEntryType() {
        EntryType.entries.forEach { type ->
            val hasRoute = VaultEditorDestinationRegistry.editRouteFor(type, "seed-token") != null

            assertEquals(hasRoute, VaultEditorDestinationRegistry.supportsFullFormEdit(type))
        }
    }
}
