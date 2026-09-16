package com.aozijx.passly.presentation.ui.shared.components

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPackagePickerBoundaryTest {
    @Test
    fun `installed app icon state comes from core platform ui`() {
        val detailBinding = source(
            "presentation/feature/vault/detail/binding/DetailAssociationsBinding.kt",
        )
        val vaultItemIcon = source(
            "presentation/ui/shared/components/VaultItemIcon.kt",
        )

        listOf(detailBinding, vaultItemIcon).forEach { consumer ->
            assertFalse(consumer.contains("app.platform.packageinfo.rememberInstalledAppIconBitmap"))
            assertTrue(consumer.contains("core.platform.packageinfo.rememberInstalledAppIconBitmap"))
        }
    }
    @Test
    fun `package picker ui does not load platform data`() {
        val picker = source(
            "presentation/ui/shared/components/AppPackagePickerBottomSheet.kt",
        )
        val associations = source(
            "presentation/ui/vault/detail/component/AssociatedInfoSection.kt",
        )

        val forbiddenPlatformLoadingTokens = listOf(
            "EntryPointAccessors",
            "InstalledAppServicesProvider",
            "Dispatchers",
            "withContext",
            "produceState",
        )
        mapOf("picker" to picker, "associations" to associations).forEach { (name, source) ->
            forbiddenPlatformLoadingTokens.forEach { forbidden ->
                assertFalse("$name UI must not reference $forbidden", source.contains(forbidden))
            }
        }
        assertFalse(associations.contains("rememberAppIcon"))
        assertFalse(associations.contains("rememberAppMetadata"))
    }

    private fun source(relativePath: String): String {
        val sourceRoot = listOf(
            File("src/main/java/com/aozijx/passly"),
            File("app/src/main/java/com/aozijx/passly"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        return File(sourceRoot, relativePath).readText()
    }
}
