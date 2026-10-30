package com.aozijx.passly.feature.vault.presentation.navigation

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.shell.presentation.navigation.AppRoute
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
}
