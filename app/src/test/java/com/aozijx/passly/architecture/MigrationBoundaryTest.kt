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
}
