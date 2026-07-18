package com.aozijx.passly.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MigrationBoundaryTest {

    private val productionKotlinFiles: Sequence<File>
        get() = File("src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }

    @Test
    fun productionSourcesDoNotReferenceLegacyFeaturePackage() {
        val sourceRoot = File("src/main")
        val offenders = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "xml") }
            .filter { "com.aozijx.passly.ui.features" in it.readText() }
            .map { it.relativeTo(sourceRoot).path }
            .toList()

        assertTrue("Legacy feature package references: $offenders", offenders.isEmpty())
    }

    @Test
    fun candidateResolverDependsOnRepositoryContract() {
        val source = File(
            "src/main/java/com/aozijx/passly/core/autofill/pipeline/CandidateResolver.kt"
        ).readText()

        assertTrue("CandidateResolver must use the domain contract", "CredentialServiceRepository" in source)
        assertTrue(
            "CandidateResolver must not use the data implementation",
            "CredentialServiceRepositoryImpl" !in source
        )
    }

    @Test
    fun domainHasNoAndroidDataOrFeatureDependencies() {
        val forbidden = listOf(
            "import android.",
            "import androidx.",
            "import com.aozijx.passly.data.",
            "import com.aozijx.passly.feature."
        )
        val offenders = productionKotlinFiles
            .filter { "/domain/" in it.invariantSeparatorsPath }
            .filter { source -> forbidden.any { it in source.readText() } }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue("Domain boundary violations: $offenders", offenders.isEmpty())
    }

    @Test
    fun upperLayersDoNotReachIntoDataImplementations() {
        val guardedPackages = listOf("/core/", "/feature/", "/security/")
        val offenders = productionKotlinFiles
            .filter { source ->
                guardedPackages.any { it in source.invariantSeparatorsPath }
            }
            .filter { "import com.aozijx.passly.data." in it.readText() }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue("Data implementation leaks: $offenders", offenders.isEmpty())
    }

    @Test
    fun packageNamesMatchSourceDirectories() {
        val sourceRoot = File("src/main/java")
        val offenders = productionKotlinFiles.mapNotNull { source ->
            val declaredPackage = source.useLines { lines ->
                lines.firstOrNull { it.startsWith("package ") }
                    ?.removePrefix("package ")
                    ?.trim()
            } ?: return@mapNotNull source.relativeTo(sourceRoot).path
            val expectedDirectory = declaredPackage.replace('.', File.separatorChar)
            val actualDirectory = source.parentFile!!.relativeTo(sourceRoot).path
            source.relativeTo(sourceRoot).path.takeIf { actualDirectory != expectedDirectory }
        }.toList()

        assertTrue("Package/path mismatches: $offenders", offenders.isEmpty())
    }

    @Test
    fun migratedFeaturePresentationKeepsComposeInsideUiPackages() {
        val guardedFeaturePaths = listOf(
            "/feature/verification/",
            "/feature/settings/apppassword/",
            "/feature/settings/security/"
        )
        val offenders = productionKotlinFiles
            .filter { source -> guardedFeaturePaths.any { it in source.invariantSeparatorsPath } }
            .filter { "/ui/" !in it.invariantSeparatorsPath }
            .filter { "androidx.compose" in it.readText() }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue("Compose outside feature UI packages: $offenders", offenders.isEmpty())
    }

    @Test
    fun recoveryCodeCreationUsesFreshIdentityVerification() {
        val navHost = File(
            "src/main/java/com/aozijx/passly/ui/navigation/PasslyNavHost.kt"
        ).readText()
        val settingsBlock = navHost.substringAfter("composable(AppRoute.Settings.route)")

        assertTrue("Recovery-code settings must force reauthentication", "requestReauth(" in settingsBlock)
    }

    @Test
    fun roundedGroupUsesCommonUiWithMandatoryStableKeys() {
        val commonGroupRoot = File(
            "src/main/java/com/aozijx/passly/ui/components/group"
        )
        val roundedGroupSource = File(commonGroupRoot, "RoundedGroup.kt").readText()
        val legacyGroupRoot = File(
            "src/main/java/com/aozijx/passly/feature/settings/components"
        )

        assertTrue("RoundedGroup must live in common UI", commonGroupRoot.isDirectory)
        assertTrue(
            "Feature-specific RoundedGroup components must be removed",
            !legacyGroupRoot.exists() || legacyGroupRoot.walkTopDown().none { it.extension == "kt" }
        )
        assertTrue(
            "RoundedGroupBuilder must not return",
            !File(commonGroupRoot, "RoundedGroupBuilder.kt").exists()
        )
        assertTrue(
            "RoundedGroup keys must be mandatory strings",
            "val key: String" in roundedGroupSource
        )
        assertTrue(
            "RoundedGroup must not fall back to list indexes",
            "originalIndex" !in roundedGroupSource
        )
        assertTrue(
            "RoundedGroup position lookup must remain linear",
            ".indexOf(" !in roundedGroupSource
        )
    }

    @Test
    fun settingsSectionIsCustomizableAndLivesInCommonUi() {
        val sectionSource = File(
            "src/main/java/com/aozijx/passly/ui/components/settings/SettingsSection.kt"
        ).readText()
        val legacyParts = File(
            "src/main/java/com/aozijx/passly/feature/settings/shell/SettingsUiParts.kt"
        )

        assertTrue("SettingsUiParts must not return", !legacyParts.exists())
        assertTrue(
            "Section layout fields must be externally configurable",
            "SettingsSectionStyle" in sectionSource
        )
        assertTrue(
            "SettingsSection must accept caller-owned composable content",
            "content: @Composable ColumnScope.() -> Unit" in sectionSource
        )
    }

    @Test
    fun featuresUseCentralPermissionRequesters() {
        val offenders = productionKotlinFiles
            .filter { "/feature/" in it.invariantSeparatorsPath }
            .filter { source ->
                val text = source.readText()
                "Manifest.permission." in text ||
                        "ActivityResultContracts.RequestPermission" in text ||
                        "ActivityResultContracts.RequestMultiplePermissions" in text
            }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue(
            "Feature permission requests bypass the central module: $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun notificationPermissionIsRequestedOnlyFromItsSetting() {
        val mainActivity = File("src/main/java/com/aozijx/passly/MainActivity.kt").readText()
        val generalSettings = File(
            "src/main/java/com/aozijx/passly/feature/settings/general/GeneralDetail.kt"
        ).readText()

        assertTrue(
            "Startup must not request notification permission",
            "AppPermission.Notifications" !in mainActivity
        )
        assertTrue(
            "Status-bar setting must own notification permission requests",
            "permissionRequester.request(AppPermission.Notifications)" in generalSettings
        )
    }

    @Test
    fun securitySettingsOwnToastPreferences() {
        val securityToasts = File(
            "src/main/java/com/aozijx/passly/feature/settings/security/ui/SecurityToastSettingsSection.kt"
        ).readText()
        val generalNotifications = File(
            "src/main/java/com/aozijx/passly/feature/settings/general/NotificationSettingsSection.kt"
        ).readText()

        assertTrue(
            "Clipboard Toast setting must live under security",
            "clipboard_clear" in securityToasts
        )
        assertTrue(
            "App-close Toast setting must live under security",
            "app_close" in securityToasts
        )
        assertTrue(
            "General notification settings must not contain Toast controls",
            "toasts." !in generalNotifications
        )
    }
}
