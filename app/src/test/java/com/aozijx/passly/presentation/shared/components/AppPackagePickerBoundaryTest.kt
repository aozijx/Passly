package com.aozijx.passly.presentation.shared.components

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPackagePickerBoundaryTest {
    @Test
    fun `installed app icon state comes from core platform ui`() {
        val detailContent = source("presentation/feature/vault/detail/ui/DetailContent.kt")
        val vaultItemIcon = source("presentation/shared/components/VaultItemIcon.kt")

        listOf(detailContent, vaultItemIcon).forEach { consumer ->
            assertFalse(consumer.contains("app.platform.packageinfo.rememberInstalledAppIconBitmap"))
            assertTrue(consumer.contains("core.platform.packageinfo.rememberInstalledAppIconBitmap"))
        }
    }

    @Test
    fun `package picker consumes mapped data and emits semantic ui events`() {
        val picker = source("presentation/shared/components/AppPackagePickerBottomSheet.kt")
        val associations = source("presentation/feature/vault/detail/ui/component/AssociatedInfoSection.kt")
        val detailContent = source("presentation/feature/vault/detail/ui/DetailContent.kt")

        val forbiddenPlatformLoadingTokens = listOf(
            "EntryPointAccessors",
            "InstalledAppServicesProvider",
            "InstalledAppDirectory",
        )
        mapOf(
            "picker" to picker,
            "associations" to associations,
            "detailContent" to detailContent,
        ).forEach { (name, source) ->
            forbiddenPlatformLoadingTokens.forEach { forbidden ->
                assertFalse("$name UI must not reference $forbidden", source.contains(forbidden))
            }
        }
        assertFalse(associations.contains("rememberAppIcon"))
        assertFalse(associations.contains("rememberAppMetadata"))
        assertTrue(detailContent.contains("model.associatedApps"))
        assertTrue(detailContent.contains("model.packagePickerApps"))
        assertTrue(detailContent.contains("onAction(DetailUiAction.LoadPackagePickerApps)"))
        assertTrue(
            detailContent.contains(
                "onAction(DetailUiAction.SelectAssociatedPackage(it.packageName))",
            ),
        )
    }

    private fun source(relativePath: String): String {
        val sourceRoot = listOf(
            File("src/main/java/com/aozijx/passly"),
            File("app/src/main/java/com/aozijx/passly"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        return File(sourceRoot, relativePath).readText()
    }
}
