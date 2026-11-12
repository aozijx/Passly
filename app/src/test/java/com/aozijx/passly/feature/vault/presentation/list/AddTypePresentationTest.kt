package com.aozijx.passly.feature.vault.presentation.list

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.aozijx.passly.R
import com.aozijx.passly.app.navigation.AppRoute
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.presentation.editor.VaultEditorCapabilities
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultAddTypePresentation
import com.aozijx.passly.feature.vault.presentation.list.ui.model.descriptionRes
import com.aozijx.passly.feature.vault.presentation.list.ui.model.icon
import com.aozijx.passly.feature.vault.presentation.list.ui.model.labelRes
import com.aozijx.passly.feature.vault.presentation.navigation.VaultEditorDestinationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class AddTypePresentationTest {
    @Test
    fun everyTypePreservesItsDisplayMetadataAndFeatureMeaning() {
        val expected =
            listOf(
                Expected(
                    AddType.PASSWORD,
                    R.string.password_label,
                    R.string.vault_add_type_password_description,
                    Icons.Default.Key,
                ),
                Expected(
                    AddType.TOTP,
                    R.string.vault_add_type_totp,
                    R.string.vault_add_type_totp_description,
                    Icons.Default.Pin,
                ),
                Expected(
                    AddType.BANK_CARD,
                    R.string.vault_fab_bank_card,
                    R.string.vault_add_type_bank_card_description,
                    Icons.Default.CreditCard,
                ),
                Expected(
                    AddType.WIFI,
                    R.string.vault_fab_wifi,
                    R.string.vault_add_type_wifi_description,
                    Icons.Default.Wifi,
                ),
                Expected(
                    AddType.SSH_KEY,
                    R.string.vault_fab_ssh_key,
                    R.string.vault_add_type_ssh_key_description,
                    Icons.Default.VpnKey,
                ),
                Expected(
                    AddType.ID_CARD,
                    R.string.vault_fab_id_card,
                    R.string.vault_add_type_id_card_description,
                    Icons.Default.Badge,
                ),
                Expected(
                    AddType.SEED_PHRASE,
                    R.string.seed_phrase,
                    R.string.vault_add_type_seed_phrase_description,
                    Icons.AutoMirrored.Filled.TextSnippet,
                ),
                Expected(
                    AddType.PASSKEY,
                    R.string.vault_fab_passkey,
                    R.string.vault_add_type_passkey_description,
                    Icons.Default.Fingerprint,
                ),
                Expected(
                    AddType.RECOVERY_CODE,
                    R.string.recovery_code_label,
                    R.string.vault_add_type_recovery_code_description,
                    Icons.Default.Restore,
                ),
            )

        assertEquals(AddType.entries.toSet(), expected.map { it.type }.toSet())
        expected.forEach { fixture ->
            assertEquals(fixture.label, fixture.type.labelRes)
            assertEquals(fixture.description, fixture.type.descriptionRes)
            assertEquals(fixture.icon, fixture.type.icon())
        }
    }

    @Test
    fun allTypesCanBeFilteredButOnlyFourHaveCreationDestinations() {
        val expectedRoutes =
            listOf(
                AddType.PASSWORD to AppRoute.AddPassword.route,
                AddType.TOTP to AppRoute.AddOtp.route,
                AddType.BANK_CARD to AppRoute.AddBankCard.route,
                AddType.WIFI to AppRoute.AddWifi.route,
                AddType.SSH_KEY to null,
                AddType.ID_CARD to null,
                AddType.SEED_PHRASE to null,
                AddType.PASSKEY to null,
                AddType.RECOVERY_CODE to null,
            )

        assertEquals(AddType.entries.toSet(), expectedRoutes.map { it.first }.toSet())
        expectedRoutes.forEach { (type, route) ->
            assertEquals(route, VaultEditorDestinationRegistry.routeFor(type))
        }
        assertEquals(expectedRoutes.filter { it.second != null }.map { it.first }, VaultEditorCapabilities.creatableTypes)
        assertEquals(listOf(AddType.TOTP, AddType.PASSWORD), VaultAddTypePresentation.fabMenuOptions)
    }

    private data class Expected(
        val type: AddType,
        val label: Int,
        val description: Int,
        val icon: ImageVector,
    )
}
