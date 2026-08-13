package com.aozijx.passly.architecture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MigrationBoundaryTest {

    private val projectRoot = File("..").canonicalFile
    private val appKotlinRoot = File("src/main/java").canonicalFile
    private val moduleKotlinRoots = listOf(
        appKotlinRoot,
        File(projectRoot, "core/android/src/main/kotlin"),
        File(projectRoot, "core/common/src/main/kotlin"),
        File(projectRoot, "core/crypto/src/main/kotlin"),
        File(projectRoot, "core/telemetry/src/main/kotlin"),
        File(projectRoot, "core/ui/src/main/kotlin"),
        File(projectRoot, "data/src/main/java"),
        File(projectRoot, "domain/src/main/kotlin"),
        File(projectRoot, "runtime/session/src/main/kotlin"),
    )

    private val productionKotlinFiles: Sequence<File>
        get() = moduleKotlinRoots.asSequence().flatMap { root ->
            root.walkTopDown().filter { it.isFile && it.extension == "kt" }
        }

    private fun moduleSource(relativePath: String): File =
        moduleKotlinRoots
            .asSequence()
            .map { root -> File(root, relativePath) }
            .firstOrNull(File::isFile)
            ?: error("Source file not found in configured modules: $relativePath")

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
    fun upperLayersDoNotReachIntoDataImplementations() {
        val guardedPackages = listOf("/core/", "/feature/", "/security/")
        val implementationImports = listOf(
            "import com.aozijx.passly.data.local.",
            "import com.aozijx.passly.data.codec.",
            "import com.aozijx.passly.data.mapper.",
            "import com.aozijx.passly.data.repository.entry.",
            "import com.aozijx.passly.data.repository.attachment.",
            "import com.aozijx.passly.data.repository.otp.",
        )
        val exemptPaths = listOf(
            "/app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/snapshot/"
        )
        val offenders = productionKotlinFiles
            .filterNot { source ->
                source.invariantSeparatorsPath.startsWith(
                    File(projectRoot, "data").invariantSeparatorsPath
                )
            }
            .filter { source ->
                guardedPackages.any { it in source.invariantSeparatorsPath }
            }
            .filter { source ->
                exemptPaths.none { it in source.invariantSeparatorsPath }
            }
            .filter { source -> implementationImports.any { it in source.readText() } }
            .map { it.relativeTo(projectRoot).path }
            .toList()

        assertTrue("Data implementation leaks: $offenders", offenders.isEmpty())
    }

    @Test
    fun appOnlyImportsDataInternalsThroughBackupSnapshotAdapter() {
        val allowedPath =
            "/com/aozijx/passly/feature/backup/internal/archive/snapshot/"
        val implementationImports = listOf(
            "import com.aozijx.passly.data.local.",
            "import com.aozijx.passly.data.codec.",
            "import com.aozijx.passly.data.mapper.",
            "import com.aozijx.passly.data.repository.entry.",
            "import com.aozijx.passly.data.repository.attachment.",
            "import com.aozijx.passly.data.repository.otp.",
        )
        val offenders = appKotlinRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { source -> implementationImports.any { it in source.readText() } }
            .filterNot { allowedPath in it.invariantSeparatorsPath }
            .map { it.relativeTo(appKotlinRoot).path }
            .toList()

        assertTrue(
            "Only Backup's database snapshot adapter may access data implementation: $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun dataModuleContainsOnlyDataOwnedPackages() {
        val dataRoot = File(projectRoot, "data/src/main/java")
        val offenders = dataRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { source ->
                source.useLines { lines ->
                    lines.firstOrNull { it.startsWith("package ") }
                        ?.startsWith("package com.aozijx.passly.data") == true
                }
            }
            .map { it.relativeTo(projectRoot).path }
            .toList()

        assertTrue("Non-data packages inside :data: $offenders", offenders.isEmpty())
    }

    @Test
    fun dataModuleDoesNotReExportImplementationDependencies() {
        val buildFile = File(projectRoot, "data/build.gradle.kts").readText()
        val reExportedDependencies = Regex("""(?m)^\s*api\(""")
            .findAll(buildFile)
            .map { it.value.trim() }
            .toList()

        assertTrue(
            "The implementation-only :data module must not re-export dependencies: " +
                reExportedDependencies,
            reExportedDependencies.isEmpty()
        )
    }

    @Test
    fun featureImplementationsStayInsideApp() {
        val settingsFile = File(projectRoot, "settings.gradle.kts").readText()
        val standaloneFeatureDirectories = File(projectRoot, "feature")
            .takeIf(File::exists)
            ?.walkTopDown()
            ?.filter { file ->
                file.isFile &&
                    (file.name == "build.gradle.kts" || "/src/" in file.invariantSeparatorsPath)
            }
            ?.map { it.relativeTo(projectRoot).path }
            ?.toList()
            .orEmpty()

        assertFalse(
            "Business features must remain package slices inside :app",
            Regex("""include\(\s*[\"']:(?:feature:|backup)[^\"']*[\"']\s*\)""")
                .containsMatchIn(settingsFile),
        )
        assertTrue(
            "Standalone feature module files must be removed: $standaloneFeatureDirectories",
            standaloneFeatureDirectories.isEmpty(),
        )
    }

    @Test
    fun backupIsOwnedByTheAppFeature() {
        val dataBackupRoot = File(projectRoot, "data/src/main/java/com/aozijx/passly/data/backup")
        val appBackupRoot = File(
            "src/main/java/com/aozijx/passly/feature/backup/internal/archive"
        )

        assertFalse("Backup must not be implemented by :data", dataBackupRoot.exists())
        assertTrue("Backup archive implementation must live in app feature.backup", appBackupRoot.isDirectory)
    }

    @Test
    fun vaultFeatureDoesNotDependOnSiblingFeaturesOrInternalBucket() {
        val vaultRoot = File("src/main/java/com/aozijx/passly/feature/vault")
        val siblingFeatureImport = Regex(
            """import com\.aozijx\.passly\.feature\.(?!vault(?:\.|$))"""
        )
        val siblingFeatureOffenders = vaultRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { siblingFeatureImport.containsMatchIn(it.readText()) }
            .map { it.relativeTo(vaultRoot).path }
            .toList()
        val internalBucket = File(vaultRoot, "internal")
        val internalFiles = if (internalBucket.exists()) {
            internalBucket.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .map { it.relativeTo(vaultRoot).path }
                .toList()
        } else {
            emptyList()
        }

        assertTrue(
            "Vault imports sibling features: $siblingFeatureOffenders",
            siblingFeatureOffenders.isEmpty()
        )
        assertTrue(
            "Vault internal bucket must stay split by responsibility: $internalFiles",
            internalFiles.isEmpty()
        )
    }

    @Test
    fun vaultMenuStateIsTransientAndEntryCreationUsesDedicatedRoutes() {
        val vaultUiState = File(
            "src/main/java/com/aozijx/passly/feature/vault/contract/VaultUiState.kt"
        ).readText()
        val vaultReducer = File(
            "src/main/java/com/aozijx/passly/feature/vault/presentation/VaultReducer.kt"
        ).readText()
        val topBar = File(
            "src/main/java/com/aozijx/passly/feature/vault/components/topbar/VaultTopBar.kt"
        ).readText()
        val routeSource = File(
            "src/main/java/com/aozijx/passly/app/navigation/AppRoute.kt"
        ).readText()
        val passwordScreen = File(
            "src/main/java/com/aozijx/passly/feature/vault/editor/password/" +
                    "AddPasswordScreen.kt"
        )
        val otpScreen = File(
            "src/main/java/com/aozijx/passly/feature/vault/editor/otp/" +
                    "AddOtpScreen.kt"
        )
        val commonScaffold = File(
            "src/main/java/com/aozijx/passly/feature/vault/editor/common/" +
                    "AddEntryScaffold.kt"
        ).readText()
        val oldPasswordDialog = File(
            "src/main/java/com/aozijx/passly/feature/vault/components/editor/" +
                    "AddPasswordDialog.kt"
        )
        val oldOtpDialog = File(
            "src/main/java/com/aozijx/passly/feature/vault/components/editor/" +
                    "AddOtpDialog.kt"
        )

        assertTrue(
            "Popup visibility must not survive in VaultViewModel state",
            "isMoreMenuExpanded" !in vaultUiState &&
                    "isMoreMenuExpanded" !in vaultReducer
        )
        assertTrue(
            "Top bar must own popup visibility locally",
            "var isMoreMenuExpanded by remember" in topBar
        )
        assertTrue(
            "Password and OTP creation must have dedicated navigation routes",
            "data object AddPassword" in routeSource &&
                    "data object AddOtp" in routeSource &&
                    passwordScreen.isFile &&
                    otpScreen.isFile
        )
        assertTrue(
            "Password and OTP creation dialogs must not return",
            !oldPasswordDialog.exists() && !oldOtpDialog.exists()
        )
        assertTrue(
            "Entry creation pages must share the page shell and FAB transition",
            "fun AddEntryScaffold" in commonScaffold &&
                    "sharedBounds" in commonScaffold &&
                    "ADD_ENTRY_FAB_SHARED_KEY" in commonScaffold
        )
    }

    @Test
    fun associatedPackagesStayEncryptedAndVisibleInEntryDetails() {
        val entryEntity = File(
            "../data/src/main/java/com/aozijx/passly/data/local/database/entity/EntryEntity.kt"
        ).readText()
        val summaryPayload = moduleSource(
            "com/aozijx/passly/data/codec/entry/payload/SummaryPayload.kt"
        ).readText()
        val associatedInfoSection = File(
            "src/main/java/com/aozijx/passly/feature/detail/ui/sections/" +
                    "AssociatedInfoSection.kt"
        ).readText()

        assertTrue(
            "Associated packages must remain inside encrypted summary payloads",
            "packageNames: Set<String>" in summaryPayload &&
                    "val packageName" !in entryEntity
        )
        assertTrue(
            "Login details must expose associated package names",
            "entry.associations.applicationIds" in associatedInfoSection &&
                    "AssociatedAppRow" in associatedInfoSection
        )
    }

    @Test
    fun appPasswordFeedbackAndEntryAuthenticationStaySeparated() {
        val passwordFields = File(
            "src/main/java/com/aozijx/passly/core/ui/components/apppassword/" +
                    "PasswordFields.kt"
        ).readText()
        val authenticationHost = File(
            "src/main/java/com/aozijx/passly/core/ui/components/auth/AuthenticationHost.kt"
        ).readText()
        val authenticationScreen = File(
            "src/main/java/com/aozijx/passly/feature/auth/ui/AuthenticationScreen.kt"
        ).readText()
        val addPasswordScreen = File(
            "src/main/java/com/aozijx/passly/feature/vault/editor/password/" +
                    "AddPasswordScreen.kt"
        ).readText()
        val provisioner = File(
            "src/main/java/com/aozijx/passly/security/authentication/" +
                    "DefaultAuthenticationMethodProvisioner.kt"
        ).readText()
        val authenticationModels = moduleSource(
            "com/aozijx/passly/domain/access/model/Authentication.kt"
        ).readText()
        val mainScreen = File(
            "src/main/java/com/aozijx/passly/app/shell/ui/AppShell.kt"
        ).readText()
        val inputActionButton = File(
            "../core/ui/src/main/kotlin/com/aozijx/passly/core/ui/components/common/" +
                    "InputActionButton.kt"
        ).readText()
        val authenticationViewModel = File(
            "src/main/java/com/aozijx/passly/feature/auth/presentation/" +
                    "AuthenticationViewModel.kt"
        ).readText()
        val authenticationUiState = File(
            "src/main/java/com/aozijx/passly/feature/auth/contract/" +
                    "AuthenticationUiState.kt"
        ).readText()

        assertTrue(
            "Password inputs must use the platform password input contract",
            "keyboardType = KeyboardType.Password" in passwordFields &&
                    "keyboardType = KeyboardType.Password" in authenticationHost
        )
        assertTrue(
            "Authentication failures must stay authoritative from AuthenticationManager",
            "result = state.result" in inputActionButton &&
                    "RESULT_DISPLAY_DURATION_MS" in inputActionButton &&
                    "val failure: AuthenticationFailure" in authenticationUiState &&
                    "AuthenticationUiError" !in authenticationUiState &&
                    "toUiError" !in authenticationViewModel
        )
        assertTrue(
            "Expanded authentication inputs must remain visible above the IME",
            "imePadding()" in authenticationScreen &&
                    "bringIntoViewRequester.bringIntoView()" in inputActionButton &&
                    "keyboardController?.hide()" in inputActionButton &&
                    "focusManager.clearFocus()" in inputActionButton
        )
        assertTrue(
            "Changing an app password must verify the current password",
            "currentPassword: CharArray" in provisioner &&
                    "AuthenticationPurpose.MANAGE_APP_PASSWORD" in provisioner
        )
        assertTrue(
            "Creating a vault entry must not introduce a second authentication gate",
            "AuthenticationPurpose" !in addPasswordScreen &&
                    "CREATE_ENTRY" !in authenticationModels
        )
        assertTrue(
            "FLAG_SECURE must remain controlled only by the global setting",
            "if (mainConfig.isSecureContentEnabled)" in mainScreen &&
                    "SensitiveContent" !in mainScreen
        )
    }

    @Test
    fun packageNamesMatchSourceDirectories() {
        val offenders = productionKotlinFiles.mapNotNull { source ->
            val sourceRoot = moduleKotlinRoots.first { root ->
                source.toPath().startsWith(root.toPath())
            }
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
    fun viewModelsDoNotOwnTopLevelMviContracts() {
        val topLevelContractPattern = Regex("""(?m)^(data class|sealed interface|sealed class)\s+""")
        val offenders = productionKotlinFiles
            .filter { it.name.endsWith("ViewModel.kt") }
            .filter { topLevelContractPattern.containsMatchIn(it.readText()) }
            .map { it.relativeTo(projectRoot).path }
            .toList()

        assertTrue(
            "Move ViewModel-owned MVI contracts into dedicated contract files: $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun migratedFeaturePresentationKeepsComposeInsideUiPackagesOrFeatureScreens() {
        val guardedFeaturePaths = listOf(
            "/feature/verification/",
            "/feature/settings/apppassword/",
            "/feature/settings/security/"
        )
        val offenders = productionKotlinFiles
            .filter { source -> guardedFeaturePaths.any { it in source.invariantSeparatorsPath } }
            .filter {
                "/ui/" !in it.invariantSeparatorsPath &&
                        !it.name.endsWith("Screen.kt")
            }
            .filter { "androidx.compose" in it.readText() }
            .map { it.relativeTo(projectRoot).path }
            .toList()

        assertTrue(
            "Compose outside UI packages or feature Screen entry points: $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun vaultQuickFilterModelDoesNotOwnUiPresentation() {
        val vaultQuickFilter = moduleSource(
            "com/aozijx/passly/data/settings/model/LibraryQuickFilter.kt"
        ).readText()

        assertTrue(
            "LibraryQuickFilter must stay a pure filtering/settings model",
            "androidx.compose" !in vaultQuickFilter &&
                    "com.aozijx.passly.R" !in vaultQuickFilter &&
                    "ImageVector" !in vaultQuickFilter &&
                    "titleRes" !in vaultQuickFilter
        )
    }

    @Test
    fun addTypeModelDoesNotOwnUiPresentation() {
        val addType = File(
            "src/main/java/com/aozijx/passly/feature/vault/model/AddType.kt"
        ).readText()

        assertTrue(
            "AddType must stay a pure creation-type model",
            "androidx.compose" !in addType &&
                    "com.aozijx.passly.R" !in addType &&
                    "ImageVector" !in addType &&
                    "labelRes" !in addType
        )
    }

    @Test
    fun recoveryCodeCreationUsesFreshIdentityVerification() {
        val recoveryDraftViewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/security/RecoveryDraftViewModel.kt"
        ).readText()

        assertTrue(
            "Recovery-code module must request its own fresh authentication",
            "AuthenticationPurpose.MANAGE_RECOVERY_CODE" in recoveryDraftViewModel &&
                    "authenticationManager.authenticate(" in recoveryDraftViewModel
        )
    }

    @Test
    fun roundedGroupUsesCommonUiWithMandatoryStableKeys() {
        val commonGroupRoot = File(
            "src/main/java/com/aozijx/passly/core/ui/components/group"
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
            "../core/ui/src/main/kotlin/com/aozijx/passly/core/ui/components/settings/SettingsSection.kt"
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
            .map { it.relativeTo(projectRoot).path }
            .toList()

        assertTrue(
            "Feature permission requests bypass the central module: $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun notificationPermissionIsRequestedOnlyFromItsSetting() {
        val mainActivity = File("src/main/java/com/aozijx/passly/MainActivity.kt").readText()
        val notificationSettings = File(
            "src/main/java/com/aozijx/passly/feature/settings/general/NotificationDetail.kt"
        ).readText()

        assertTrue(
            "Startup must not request notification permission",
            "request(RuntimePermission.POST_NOTIFICATIONS)" !in mainActivity
        )
        assertTrue(
            "Message setting must own notification permission requests",
            "permissionHost.request(RuntimePermission.POST_NOTIFICATIONS)" in notificationSettings
        )
    }

    @Test
    fun backupFeatureOwnsItsUiAndUsesBottomSheets() {
        val vaultRoot = File("src/main/java/com/aozijx/passly/feature/vault")
        val vaultOffenders = vaultRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter {
                val text = it.readText()
                "BackupViewModel" in text ||
                        "BackupAction" in text ||
                    "BackupPasswordDialog" in text ||
                    "CustomExportMenuItem" in text
            }
            .map { it.relativeTo(vaultRoot).path }
            .toList()
        val sheetSource = File(
            "src/main/java/com/aozijx/passly/feature/backup/internal/ui/BackupRestoreSheets.kt"
        ).readText()

        assertTrue("Vault must not own backup actions: $vaultOffenders", vaultOffenders.isEmpty())
        assertTrue("Backup UI must use Material bottom sheets", "ModalBottomSheet(" in sheetSource)
        assertTrue(
            "Format picker must expose all first-party exports",
            listOf("ENCRYPTED", "JSON", "TEXT").all { it in sheetSource }
        )
    }

    @Test
    fun legacyBackupUiCannotReturn() {
        val removedFiles = listOf(
            File("src/main/java/com/aozijx/passly/core/ui/components/BackupPasswordDialog.kt"),
            File("src/main/java/com/aozijx/passly/core/ui/components/PlainExportDialog.kt"),
            File("src/main/java/com/aozijx/passly/core/util/PlainExportTokenManager.kt"),
            File(
                "src/main/java/com/aozijx/passly/feature/vault/components/topbar/CustomExportMenuItem.kt"
            )
        )

        assertTrue(
            "Legacy backup UI files must stay deleted",
            removedFiles.none(File::exists)
        )
    }

    @Test
    fun messageSettingsOwnTopicPreferences() {
        val removedSecurityToasts = File(
            "src/main/java/com/aozijx/passly/feature/settings/security/ui/SecurityToastSettingsSection.kt"
        )
        val generalNotifications = File(
            "src/main/java/com/aozijx/passly/feature/settings/general/NotificationSettingsSection.kt"
        ).readText()

        assertTrue(
            "Legacy security Toast settings must be removed",
            !removedSecurityToasts.exists()
        )
        assertTrue(
            "General message settings must expose topic controls",
            "NoticeTopic.entries" in generalNotifications
        )
    }

    @Test
    fun legacyMessageAndPermissionStacksCannotReturn() {
        val forbiddenSymbols = listOf(
            "AppMessageCenter",
            "AppMessagePublisher",
            "AppStatusBarNotifier",
            "AppMessagePreferences",
            "AppPermission",
            "AppPermissionManager",
            "ActivityPermissionRequester",
            "PermissionManagerEntryPoint",
            "rememberAppPermissionRequester"
        )
        val offenders = productionKotlinFiles
            .filter { source -> forbiddenSymbols.any { it in source.readText() } }
            .map { it.relativeTo(projectRoot).path }
            .toList()

        assertTrue("Legacy message/permission references: $offenders", offenders.isEmpty())
    }

    @Test
    fun domainDoesNotEmitTelemetryOrUserMessages() {
        val forbidden = listOf("TelemetryReporter", "AppNoticePublisher", "AppLog")
        val offenders = productionKotlinFiles
            .filter { "/domain/" in it.invariantSeparatorsPath }
            .filter { "/domain/notice/port/" !in it.invariantSeparatorsPath }
            .filter { source -> forbidden.any { it in source.readText() } }
            .map { it.relativeTo(projectRoot).path }
            .toList()

        assertTrue("Domain side effects: $offenders", offenders.isEmpty())
    }

    @Test
    fun legacyAuthenticationStackCannotReturn() {
        val forbiddenSymbols = listOf(
            "VerificationGateway",
            "BiometricPromptLauncher",
            "BiometricAuthCoordinator",
            "UserSessionManager",
            "AppIdleMonitor"
        )
        val offenders = productionKotlinFiles
            .filter { source -> forbiddenSymbols.any { it in source.readText() } }
            .map { it.relativeTo(projectRoot).path }
            .toList()

        assertTrue("Legacy authentication references: $offenders", offenders.isEmpty())
    }

    @Test
    fun authenticationActivitiesStayInTheApplicationProcess() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(
            "Authentication hosts must not be moved to another process",
            "android:process" !in manifest
        )
    }

    @Test
    fun legacyAutofillAuthenticationReturnsTheMatchingPlatformType() {
        val activity = File(
            "src/main/java/com/aozijx/passly/feature/autofill/framework/" +
                    "AutofillFillActivity.kt"
        ).readText()
        val viewModel = File(
            "src/main/java/com/aozijx/passly/feature/autofill/framework/" +
                    "AutofillFillViewModel.kt"
        ).readText()
        val responseFactory = File(
            "src/main/java/com/aozijx/passly/service/autofill/framework/builder/" +
                    "LegacyResponseFactory.kt"
        ).readText()

        assertTrue(
            "Dataset authentication must return a Dataset for immediate filling",
            "EXTRA_RETURN_DATASET" in activity &&
                    "AutofillAuthenticationPayload.DatasetResult(dataset)" in viewModel &&
                    "AutofillAuthenticationPayload.Response(" in viewModel
        )
        assertTrue(
            "The authentication Activity must accept both platform payload types",
            "is AutofillAuthenticationPayload.Response" in activity &&
                    "is AutofillAuthenticationPayload.DatasetResult" in activity &&
                    "EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET" in activity
        )
        assertTrue(
            "Autofill authentication PendingIntents must remain mutable for platform extras",
            "PendingIntent.FLAG_MUTABLE" in responseFactory &&
                    "PendingIntent.FLAG_IMMUTABLE" !in responseFactory
        )
    }

    @Test
    fun credentialProviderUsesSystemFinalRequestsAndKeepsPasskeysDisabled() {
        val responseViewModel = File(
            "src/main/java/com/aozijx/passly/feature/autofill/credential/" +
                    "CredentialResponseViewModel.kt"
        ).readText()
        val responseActivity = File(
            "src/main/java/com/aozijx/passly/feature/autofill/credential/" +
                    "CredentialResponseActivity.kt"
        ).readText()
        val requestParser = File(
            "src/main/java/com/aozijx/passly/feature/autofill/credential/" +
                    "CredentialRequestParser.kt"
        ).readText()
        val pendingIntentFactory = File(
            "src/main/java/com/aozijx/passly/service/autofill/credential/" +
                    "CredentialPendingIntentFactory.kt"
        ).readText()
        val createHandler = File(
            "src/main/java/com/aozijx/passly/service/autofill/credential/" +
                    "CredentialBeginCreateHandler.kt"
        ).readText()
        val adapter = File(
            "src/main/java/com/aozijx/passly/service/autofill/credential/" +
                    "CredentialPlatformAdapter.kt"
        ).readText()
        val responseUseCases = moduleSource(
            "com/aozijx/passly/feature/autofill/usecase/CredentialResponseUseCases.kt"
        ).readText()
        val providerConfig = File("src/main/res/xml/credential_service_config.xml").readText()

        assertTrue(
            "Final get/create phases must trust the system-injected provider request",
            "CredentialRequestParser.parsePasswordGet" in responseViewModel &&
                    "CredentialRequestParser.parsePasswordCreate" in responseViewModel &&
                    "retrieveProviderGetCredentialRequest(sourceIntent)" in requestParser &&
                    "retrieveProviderCreateCredentialRequest(sourceIntent)" in requestParser &&
                    "EXTRA_PACKAGE_NAME" !in pendingIntentFactory
        )
        assertTrue(
            "Credential Manager valid exceptions must be returned with RESULT_OK",
            "UiState.Complete" in responseActivity &&
                    "setResult(RESULT_OK, state.resultIntent)" in responseActivity
        )
        assertTrue(
            "Password creation must complete the provider two-phase contract",
            "BeginCreatePasswordCredentialRequest" in createHandler &&
                    "createPasswordCreatePendingIntent" in createHandler &&
                    "CreatePasswordResponse" in File(
                "src/main/java/com/aozijx/passly/service/autofill/credential/" +
                        "CredentialResponseFactory.kt"
            ).readText()
        )
        assertTrue(
            "Allowed user ids must be checked before display and again before secret release",
            "option.allowedUserIds" in adapter &&
                    "allowedUserIds.isNotEmpty() && selected.profile.username !in allowedUserIds" in
                    responseUseCases
        )
        assertTrue(
            "Passkey capability must stay unpublished until WebAuthn signing is implemented",
            "android.credentials.TYPE_PASSWORD_CREDENTIAL" in providerConfig &&
                    "TYPE_PUBLIC_KEY_CREDENTIAL" !in providerConfig
        )
    }

    @Test
    fun destructiveDatabaseRecoveryRequiresFreshAuthentication() {
        val mainViewModel = File(
            "src/main/java/com/aozijx/passly/app/shell/AppShellViewModel.kt"
        ).readText()
        val settingsViewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/SettingsViewModel.kt"
        ).readText()
        val recoveryDialog = File(
            "src/main/java/com/aozijx/passly/core/ui/components/DatabaseRecoveryDialog.kt"
        ).readText()
        val controller = moduleSource(
            "com/aozijx/passly/data/repository/database/DatabaseControllerImpl.kt"
        ).readText()
        val recoveryStore = moduleSource(
            "com/aozijx/passly/data/local/database/recovery/DatabaseRecoveryStore.kt"
        ).readText()
        val authenticationManager = File(
            "src/main/java/com/aozijx/passly/security/authentication/DefaultAuthenticationManager.kt"
        ).readText()
        val authPolicy = moduleSource(
            "com/aozijx/passly/domain/access/policy/AuthenticationMethodPolicy.kt"
        ).readText()
        val credentialExecutor = File(
            "src/main/java/com/aozijx/passly/security/authentication/CredentialMethodExecutor.kt"
        ).readText()
        val sessionController = File(
            "src/main/java/com/aozijx/passly/security/authentication/VaultSessionController.kt"
        ).readText()
        val recoveryCompletion =
            sessionController.substringAfter("suspend fun completeDatabaseRecovery")
        val dataManagementViewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/datamanagement/" +
                "DataManagementSettingsViewModel.kt"
        ).readText()
        val savedRecovery = moduleSource(
            "com/aozijx/passly/data/local/database/recovery/" +
                "DatabaseRecoveryRepositoryImpl.kt"
        ).readText()

        assertTrue(
            "Crash recovery must request its non-destructive authentication purpose",
            "AuthenticationPurpose.RECOVER_DATABASE" in mainViewModel
        )
        assertTrue(
            "Permanent deletion must live behind authenticated settings",
            "AuthenticationPurpose.CLEAR_DATABASE" in settingsViewModel
        )
        assertTrue(
            "Crash dialog must not expose permanent database deletion",
            "onClearDatabase" !in recoveryDialog &&
                "database_recovery_clear_action" !in recoveryDialog
        )
        assertTrue(
            "Database recovery must preserve storage after sealing leases",
            controller.indexOf("sessionManager.seal()") <
                controller.indexOf("recoveryStore.preserveAndClearActiveVault()")
        )
        assertTrue(
            "Recovery storage must copy before clearing the active database",
            recoveryStore.indexOf("source.copyTo") <
                recoveryStore.indexOf("clearActiveDatabase(databaseFile)")
        )
        assertTrue(
            "Recovery code must remain available for damaged database recovery only",
            "AuthenticationPurpose.RECOVER_DATABASE" in authPolicy &&
                    "AuthenticationPurpose.RECOVER_DATABASE -> AuthenticationMethod.entries.toSet()" in
                    authPolicy
        )
        assertTrue(
            "Permanent database deletion must reject the recovery code",
            "AuthenticationPurpose.CLEAR_DATABASE -> PRIMARY_METHODS" in authPolicy ||
                    "CLEAR_DATABASE -> PRIMARY_METHODS" in authPolicy
        )
        assertTrue(
            "Saved database restoration must reject the recovery code",
            Regex(
                "AuthenticationPurpose\\.RESTORE_DATABASE,\\s*" +
                    "AuthenticationPurpose\\.CLEAR_DATABASE -> PRIMARY_METHODS",
            ).containsMatchIn(authPolicy)
        )
        assertTrue(
            "Cold-start recovery must stage the DEK without opening the broken database",
            "session.stageDatabaseRecovery(type, ownedDek)" in credentialExecutor
        )
        assertTrue(
            "A recovered database must open before authentication state is published",
            recoveryCompletion.indexOf("sessionManager.lockState != SecureSessionState.UNLOCKED") <
                recoveryCompletion.indexOf("markAuthenticatedInternal()")
        )
        assertTrue(
            "Saved database restoration requires a full session and fresh primary authentication",
            "hasFullSecureSessionAccess()" in dataManagementViewModel &&
                "AuthenticationPurpose.RESTORE_DATABASE" in dataManagementViewModel
        )
        assertTrue(
            "Saved database restoration must work from a copy and wipe its copied key",
            "source.copyTo" in savedRecovery &&
                "key.fill(0)" in savedRecovery &&
                "deleteWorkDatabase(workName)" in savedRecovery
        )
    }

    @Test
    fun biometricUnlockCannotPublishAuthenticatedBeforeDatabaseOpens() {
        val sessionController = File(
            "src/main/java/com/aozijx/passly/security/authentication/VaultSessionController.kt"
        ).readText()
        val biometricExecutor = File(
            "src/main/java/com/aozijx/passly/security/authentication/BiometricMethodExecutor.kt"
        ).readText()

        assertTrue(
            "Sealed biometric sessions must open the database",
            "if (lockStateManager.state == SecureSessionState.SEALED)" in sessionController &&
                "val err = sessionManager.unlock()" in sessionController
        )
        assertTrue(
            "Biometric authentication must fail when the database session cannot open",
            "if (session.markAuthenticated())" in biometricExecutor &&
                "AuthenticationFailureCode.SESSION_TRANSITION_FAILED" in biometricExecutor
        )
    }

    @Test
    fun recoveryDraftViewModelDependsOnlyOnAuthenticationContracts() {
        val source = File(
            "src/main/java/com/aozijx/passly/feature/settings/security/RecoveryDraftViewModel.kt"
        ).readText()

        assertTrue(
            "Recovery draft UI must not access cryptographic implementations",
            "import com.aozijx.passly.security." !in source
        )
        assertTrue("Recovery draft must use its domain factory", "RecoveryCredentialFactory" in source)
    }

    @Test
    fun legacyLoggingStackCannotReturn() {
        val legacyRoots = listOf(
            File("src/main/java/com/aozijx/passly/core/log"),
            File("src/main/java/com/aozijx/passly/core/diagnostics")
        )
        val forbiddenSymbols = listOf(
            "core.log.Logcat",
            "core.log.LogExporter",
            "LogFilter",
            "AppLog",
            "core.diagnostics.DiagnosticsRuntime",
            "core.diagnostics.DiagnosticsPolicy",
            "PerFileEncryptedLogSink",
            "LogSanitizer"
        )
        val legacyReferences = productionKotlinFiles
            .filter { source ->
                val text = source.readText()
                forbiddenSymbols.any(text::contains)
            }
            .map { it.relativeTo(projectRoot).path }
            .toList()

        assertTrue("Legacy logging references: $legacyReferences", legacyReferences.isEmpty())
        assertTrue(
            "Legacy logging source directory must be removed",
            legacyRoots.all { root ->
                !root.exists() || root.walkTopDown().none { it.extension == "kt" }
            }
        )
    }

    @Test
    fun encryptedDiagnosticsUseBoundedQueueAndPreparedCrashKey() {
        val source = moduleSource(
            "com/aozijx/passly/data/diagnostics/EncryptedLogStore.kt"
        ).readText()
        val emergencyBlock = source
            .substringAfter("fun crashEmergencyWrite")
            .substringBefore("private fun fallbackEmergencyWrite")

        assertTrue("Log writer queue must remain bounded", "ArrayBlockingQueue" in source)
        assertTrue("Every diagnostics file needs an encrypted header", "writeHeader(file" in source)
        assertTrue(
            "Crash writer must use the preloaded key instead of Keystore",
            "getOrCreateWrappingKey" !in emergencyBlock
        )
        assertTrue(
            "Crash writer must not wait for the normal writer lock",
            "lockWrites = false" in emergencyBlock
        )
        assertTrue("Every record must authenticate sequence and level", "buildRecordAad" in source)
        assertTrue("Record nonce must have an exact length", "checkedExact(NONCE_BYTES)" in source)
    }

    @Test
    fun platformEffectsStayBehindTheirSingleGateway() {
        val directLogOffenders = productionKotlinFiles
            .filter {
                it.invariantSeparatorsPath
                    .endsWith("/core/telemetry/AndroidLogSink.kt")
                    .not()
            }
            .filter { "android.util.Log" in it.readText() || "printStackTrace(" in it.readText() }
            .map { it.relativeTo(projectRoot).path }
            .toList()
        val directNotificationOffenders = productionKotlinFiles
            .filter {
                it.invariantSeparatorsPath
                    .endsWith("/app/message/system/AndroidSystemNotificationGateway.kt")
                    .not()
            }
            .filter {
                val text = it.readText()
                "NotificationManagerCompat" in text || "NotificationCompat." in text
            }
            .map { it.relativeTo(projectRoot).path }
            .toList()
        val directPermissionOffenders = productionKotlinFiles
            .filter {
                val path = it.invariantSeparatorsPath
                !path.endsWith("/core/permission/catalog/RuntimePermissionCatalog.kt") &&
                    !path.endsWith("/core/permission/compose/PermissionRequestHost.kt")
            }
            .filter {
                val text = it.readText()
                "Manifest.permission." in text ||
                    "ActivityResultContracts.RequestPermission" in text
            }
            .map { it.relativeTo(projectRoot).path }
            .toList()

        assertTrue("Direct logging bypasses AndroidLogSink: $directLogOffenders", directLogOffenders.isEmpty())
        assertTrue(
            "Direct notification bypasses AndroidSystemNotificationGateway: $directNotificationOffenders",
            directNotificationOffenders.isEmpty()
        )
        assertTrue(
            "Direct runtime permission bypasses the permission center: $directPermissionOffenders",
            directPermissionOffenders.isEmpty()
        )
    }

    @Test
    fun newCentersContainNoStubOrTodoImplementation() {
        val scopedRoots = listOf(
            File("src/main/java/com/aozijx/passly/app/message"),
            File("src/main/java/com/aozijx/passly/app/permission"),
            File("src/main/java/com/aozijx/passly/app/diagnostics"),
            File("src/main/java/com/aozijx/passly/core/permission"),
            File("src/main/java/com/aozijx/passly/app/message/runtime"),
            File(projectRoot, "data/src/main/java/com/aozijx/passly/data/diagnostics"),
            File(projectRoot, "domain/src/main/kotlin/com/aozijx/passly/domain/notice")
        )
        val offenders = scopedRoots.asSequence()
            .filter(File::exists)
            .flatMap { it.walkTopDown() }
            .filter { it.isFile && it.extension == "kt" }
            .filter {
                val text = it.readText()
                "TODO" in text || "FIXME" in text || "StubSystemNotificationGateway" in text
            }
            .map { it.relativeTo(projectRoot).path }
            .toList()

        assertTrue("Incomplete center implementations: $offenders", offenders.isEmpty())
    }

    @Test
    fun errorMessagesLiveInAppMessageMappingNotCoreError() {
        val oldPresentationDir = File(
            "src/main/java/com/aozijx/passly/core/error/presentation"
        )
        val newMappingFile = File(
            "src/main/java/com/aozijx/passly/app/message/mapping/ErrorMessages.kt"
        )

        assertTrue(
            "ErrorMessages must not remain in core/error/presentation",
            !oldPresentationDir.exists() || oldPresentationDir.walkTopDown()
                .none { it.extension == "kt" }
        )
        assertTrue(
            "ErrorMessages must live in app/message/mapping",
            newMappingFile.exists()
        )
    }

    @Test
    fun recoveryCodeCannotActAsEverydayUnlockMethod() {
        val authPolicy = moduleSource(
            "com/aozijx/passly/domain/access/policy/AuthenticationMethodPolicy.kt"
        ).readText()
        val authModels = moduleSource(
            "com/aozijx/passly/domain/access/model/Authentication.kt"
        ).readText()
        val authManager = File(
            "src/main/java/com/aozijx/passly/security/authentication/DefaultAuthenticationManager.kt"
        ).readText()
        val sessionController = File(
            "src/main/java/com/aozijx/passly/security/authentication/VaultSessionController.kt"
        ).readText()
        val credentialExecutor = File(
            "src/main/java/com/aozijx/passly/security/authentication/CredentialMethodExecutor.kt"
        ).readText()
        val provisioner = File(
            "src/main/java/com/aozijx/passly/security/authentication/DefaultAuthenticationMethodProvisioner.kt"
        ).readText()
        val provisionerInterface = moduleSource(
            "com/aozijx/passly/domain/access/port/AuthenticationMethodProvisioner.kt"
        ).readText()
        val authViewModel = File(
            "src/main/java/com/aozijx/passly/feature/auth/presentation/AuthenticationViewModel.kt"
        ).readText()
        val mainViewModel = File(
            "src/main/java/com/aozijx/passly/app/shell/AppShellViewModel.kt"
        ).readText()
        val mainUiState = File(
            "src/main/java/com/aozijx/passly/app/shell/contract/AppShellUiState.kt"
        ).readText()
        val mainScreen = File(
            "src/main/java/com/aozijx/passly/app/shell/ui/AppShell.kt"
        ).readText()
        val recoveryModeScreen = moduleSource(
            "com/aozijx/passly/feature/recovery/RecoveryModeScreen.kt"
        ).readText()
        val backupViewModel = File(
            "src/main/java/com/aozijx/passly/feature/backup/internal/presentation/BackupViewModel.kt"
        ).readText()
        val backupSessionPolicy = File(
            "src/main/java/com/aozijx/passly/feature/backup/internal/presentation/BackupSessionPolicy.kt"
        ).readText()
        val backupCoordinator = File(
            "src/main/java/com/aozijx/passly/feature/backup/internal/presentation/BackupOperationCoordinator.kt"
        ).readText()
        val securityViewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/security/SecuritySettingsViewModel.kt"
        ).readText()
        val databaseTransactions = moduleSource(
            "com/aozijx/passly/data/local/database/DatabaseTransactionRunner.kt"
        ).readText()
        val entryQueryRepository = moduleSource(
            "com/aozijx/passly/data/repository/entry/RoomEntryQueryRepository.kt"
        ).readText()
        val sensitiveFieldRepository = moduleSource(
            "com/aozijx/passly/data/repository/entry/RoomSensitiveFieldRepository.kt"
        ).readText()
        val otpConfigRepository = moduleSource(
            "com/aozijx/passly/data/repository/otp/RoomOtpConfigRepository.kt"
        ).readText()
        val attachmentRepository = moduleSource(
            "com/aozijx/passly/data/repository/attachment/FileBackedAttachmentRepository.kt"
        ).readText()
        val vaultViewModel = File(
            "src/main/java/com/aozijx/passly/feature/vault/VaultViewModel.kt"
        ).readText()
        val vaultAccessPolicy = File(
            "src/main/java/com/aozijx/passly/feature/vault/SecureSessionAccessPolicy.kt"
        ).readText()
        val createEntryViewModel = File(
            "src/main/java/com/aozijx/passly/feature/vault/editor/common/CreateEntryViewModel.kt"
        ).readText()
        val detailViewModel = File(
            "src/main/java/com/aozijx/passly/feature/detail/DetailViewModel.kt"
        ).readText()
        val detailAccessPolicy = File(
            "src/main/java/com/aozijx/passly/feature/detail/DetailAccessPolicy.kt"
        ).readText()
        val recoveryModeViewModel = moduleSource(
            "com/aozijx/passly/feature/recovery/RecoveryModeViewModel.kt"
        ).readText()
        val dataViewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/datamanagement/DataManagementSettingsViewModel.kt"
        ).readText()
        val diagnosticsViewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/general/DiagnosticsSettingsViewModel.kt"
        ).readText()

        // 1. UNLOCK_VAULT must not allow recovery code
        assertTrue(
            "UNLOCK_VAULT must use PRIMARY_METHODS only",
            "UNLOCK_VAULT," in authPolicy && "CLEAR_DATABASE -> PRIMARY_METHODS" in authPolicy
        )

        // 2. Recovery mode state must exist
        assertTrue(
            "RecoveryMode state must be defined",
            "data class RecoveryMode" in authModels
        )
        assertTrue(
            "AppShellUiState must track recovery mode",
            "isRecoveryMode: Boolean" in mainUiState
        )
        assertTrue(
            "AppShell must render RecoveryModeScreen",
            "RecoveryModeScreen(" in mainScreen
        )

        // 3. VaultSessionController must support recovery unlock
        assertTrue(
            "commitRecoveryUnlock must exist",
            "fun commitRecoveryUnlock" in sessionController
        )
        assertTrue(
            "markRecoveryMode must exist",
            "fun markRecoveryMode" in sessionController
        )
        assertTrue(
            "isRecoveryMode must check RecoveryMode state",
            "is AuthenticationState.RecoveryMode" in sessionController
        )

        // 4. CredentialMethodExecutor must route recovery purposes
        assertTrue(
            "RECOVER_AUTH_METHODS must call commitRecoveryUnlock",
            "AuthenticationPurpose.RECOVER_AUTH_METHODS" in credentialExecutor &&
                    "commitRecoveryUnlock" in credentialExecutor
        )
        assertFalse(
            "Recovery export authentication purpose must not exist",
            "RECOVERY_EXPORT" in authModels || "RECOVERY_EXPORT" in credentialExecutor
        )

        // 5. AuthenticationManager must gate recovery mode purposes
        assertTrue(
            "AuthenticationManager must check RECOVERY_MODE_PURPOSES",
            "RECOVERY_MODE_PURPOSES" in authManager
        )
        assertTrue(
            "AuthenticationManager must allow recovery mode reuse",
            "RECOVERY_MODE_REUSABLE_PURPOSES" in authManager
        )

        // 6. Provisioner must not allow recovery code as substitute for primary methods
        assertTrue(
            "disableAppPassword must use hasAlternativePrimaryFactor",
            "availabilityResolver.hasAlternativePrimaryFactor" in provisioner &&
                    "EnvelopeType.APP_PASSWORD" in provisioner
        )
        assertTrue(
            "disableBiometric must use hasAlternativePrimaryFactor",
            "availabilityResolver.hasAlternativePrimaryFactor" in provisioner &&
                    "EnvelopeType.BIOMETRIC" in provisioner
        )

        // 7. checkRecoveryCode must not produce authentication success
        assertTrue(
            "checkRecoveryCode must exist in interface",
            "suspend fun checkRecoveryCode" in provisionerInterface
        )
        assertTrue(
            "SecuritySettingsViewModel must use checkRecoveryCode",
            "methodProvisioner.checkRecoveryCode" in securityViewModel
        )

        // 8. Recovery code unlock must use RECOVER_AUTH_METHODS purpose
        assertTrue(
            "unlockWithRecoveryCode must use RECOVER_AUTH_METHODS",
            "AuthenticationPurpose.RECOVER_AUTH_METHODS" in authViewModel
        )

        // 9. Recovery mode exit must lock with RECOVERY_EXIT
        assertTrue(
            "ExitRecovery must lock with RECOVERY_EXIT",
            "LockReason.RECOVERY_EXIT" in mainViewModel
        )

        // 10. Backup requires a full authenticated session and has no recovery path.
        assertFalse(
            "Backup coordinator must not expose a recovery export path",
            "RECOVERY_EXPORT" in backupCoordinator || "isRecoveryExport" in backupCoordinator
        )
        val sensitiveUnlockPolicy = authManager
            .substringAfter("private fun AuthenticationPurpose.unlocksSensitiveDataKey")
        assertTrue(
            "Fresh high-sensitivity authentication must unlock the sensitive data key",
            "REVEAL_HIGH_SENSITIVITY_SECRET" in sensitiveUnlockPolicy
        )
        assertTrue(
            "Normal backup export must unlock the sensitive data key",
            "AuthenticationPurpose.BACKUP_EXPORT" in sensitiveUnlockPolicy
        )
        assertFalse("Recovery export must not exist", "RECOVERY_EXPORT" in sensitiveUnlockPolicy)

        // 11. Plain Vault repositories must not treat an open recovery database as full access.
        assertTrue(
            "DatabaseTransactionRunner must require full Vault access before write/read",
            "hasFullSecureSessionAccess()" in databaseTransactions &&
                    "SessionModeRestricted" in databaseTransactions
        )
        assertTrue(
            "Entry query repository must gate normal reads",
            entryQueryRepository.split("hasFullSecureSessionAccess()").size - 1 >= 3
        )
        assertTrue(
            "Sensitive field repository must gate presence and single-field reads",
            sensitiveFieldRepository.split("hasFullSecureSessionAccess()").size - 1 >= 2
        )
        assertTrue(
            "OTP config repository must gate secret reads",
            "hasFullSecureSessionAccess()" in otpConfigRepository
        )
        assertTrue(
            "Attachment repository must gate metadata and file writes",
            attachmentRepository.split("hasFullSecureSessionAccess()").size - 1 >= 3 &&
                    "SessionModeRestricted" in attachmentRepository
        )

        // 12. Sensitive ViewModels must express their session-mode boundary explicitly.
        assertTrue(
            "VaultViewModel must gate normal Vault actions",
            "SecureSessionAccessPolicy" in vaultViewModel &&
                    "SecureSessionAccessState" !in vaultViewModel &&
                    "hasFullSecureSessionAccess()" in vaultAccessPolicy &&
                    vaultViewModel.split("hasFullAccess()").size - 1 >= 4
        )
        assertTrue(
            "CreateEntryViewModel must gate entry creation",
            "SecureSessionAccessState" in createEntryViewModel &&
                    "hasFullSecureSessionAccess()" in createEntryViewModel
        )
        assertTrue(
            "DetailViewModel must gate detail reads and reveal actions",
            "DetailAccessPolicy" in detailViewModel &&
                    "SecureSessionAccessState" !in detailViewModel &&
                    "hasFullSecureSessionAccess()" in detailAccessPolicy &&
                    detailViewModel.split("hasFullAccess()").size - 1 >= 4
        )
        assertTrue(
            "RecoveryModeViewModel must only run in RecoveryMode",
            "AuthenticationState.RecoveryMode" in recoveryModeViewModel &&
                    "ensureRecoveryMode" in recoveryModeViewModel
        )
        assertFalse(
            "Recovery mode may only replace the app password",
            "BackupArchiveService" in recoveryModeViewModel ||
                    "BackupStorageSupport" in recoveryModeViewModel ||
                    "rotateBiometricPolicy" in recoveryModeViewModel
        )
        assertTrue(
            "Recovery password reset must seal recovery session instead of promoting it",
            "wasRecoveryMode" in provisioner &&
                    "LockReason.RECOVERY_EXIT" in provisioner &&
                    "session.lock(LockReason.RECOVERY_EXIT)" in provisioner
        )
        assertTrue(
            "markAuthenticated must not promote RecoveryMode to full Vault access",
            "Recovery mode must not be promoted" in sessionController &&
                    "markAuthenticatedInternal()" !in sessionController.substringAfter(
                        "if (_state.value is AuthenticationState.RecoveryMode)"
                    ).substringBefore("} else")
        )
        assertTrue(
            "BackupViewModel must require full secure-session access",
            "BackupSessionPolicy" in backupViewModel &&
                    "SecureSessionAccessState" !in backupViewModel &&
                    "hasFullSecureSessionAccess()" in backupSessionPolicy &&
                    "isRecoveryMode()" !in backupSessionPolicy
        )
        assertTrue(
            "DataManagementSettingsViewModel must gate trash operations",
            "SecureSessionAccessState" in dataViewModel &&
                    "hasFullSecureSessionAccess()" in dataViewModel
        )
        assertTrue(
            "DiagnosticsSettingsViewModel must not expose diagnostics outside full auth",
            "AuthenticationState.Authenticated" in diagnosticsViewModel
        )
    }

    @Test
    fun recoveryLifecycleConsumesCodesAndSealsAfterPasswordProvisioning() {
        val sessionController = File(
            "src/main/java/com/aozijx/passly/security/authentication/" +
                    "VaultSessionController.kt"
        ).readText()
        val provisioner = File(
            "src/main/java/com/aozijx/passly/security/authentication/" +
                    "DefaultAuthenticationMethodProvisioner.kt"
        ).readText()
        val manager = File(
            "src/main/java/com/aozijx/passly/security/authentication/" +
                    "DefaultAuthenticationManager.kt"
        ).readText()

        assertTrue(
            "Recovery success must durably consume the recovery envelope first",
            "vaultBootstrapStore.delete(EnvelopeType.RECOVERY)" in sessionController &&
                    "consumeRecoveryEnvelope()" in sessionController &&
                    sessionController.indexOf("consumeRecoveryEnvelope()") <
                    sessionController.indexOf("markRecoveryModeInternal()"),
        )
        assertTrue(
            "Recovery consumption failures must seal database and wipe the staged DEK",
            "sealStagedRecoverySession()" in sessionController &&
                    "sessionManager.seal()" in sessionController &&
                    "dekManager.lock()" in sessionController,
        )
        assertTrue(
            "Provisioning a primary password in recovery mode must seal before returning",
            "finishRecoveryPasswordProvisioning" in provisioner &&
                    "session.lock(LockReason.RECOVERY_EXIT)" in provisioner,
        )
        assertTrue(
            "Consumed recovery availability must be invalidated in the current process",
            "_methods.value.available - AuthenticationMethod.RECOVERY_CODE" in manager,
        )
    }

    @Test
    fun entryRevisionLifecycleHasPerEntryGlobalAndDeletionPolicies() {
        val revisionDao = File(
            "../data/src/main/java/com/aozijx/passly/data/local/database/dao/revision/" +
                    "EntryRevisionCommandDao.kt"
        ).readText()
        val revisionHelper = moduleSource(
            "com/aozijx/passly/data/repository/entry/command/EntryRevisionWriter.kt"
        ).readText()
        val permanentDelete = moduleSource(
            "com/aozijx/passly/data/repository/entry/command/DeleteEntryPermanentlyExecutor.kt"
        ).readText()
        val emptyTrash = moduleSource(
            "com/aozijx/passly/data/repository/entry/command/EmptyTrashExecutor.kt"
        ).readText()

        assertTrue(
            "Revision writes must enforce both per-entry and global retention",
            "REVISION_LIMIT = 50" in revisionHelper &&
                    "GLOBAL_REVISION_LIMIT = 1_000" in revisionHelper &&
                    "deleteOldVersions(entryId, REVISION_LIMIT)" in revisionHelper &&
                    "deleteOldestBeyondGlobalLimit(GLOBAL_REVISION_LIMIT)" in revisionHelper,
        )
        assertTrue(
            "Global pruning must have deterministic newest-first ordering",
            "ORDER BY createdAt DESC, revisionId DESC" in revisionDao,
        )
        assertTrue(
            "Permanent deletion paths must explicitly remove revision history",
            "entryRevisionCommandDao().deleteByEntryId(id)" in permanentDelete &&
                    "entryRevisionCommandDao().deleteForDeletedEntries()" in emptyTrash,
        )
    }

    // ============================================================
    // 阶段 0 — 护栏扩展：跨 Feature 依赖、MVI 入口、文件大小
    // ============================================================

    @Test
    fun coreDoesNotOwnBackupFeaturePresentation() {
        val leakedPresentationFiles = listOf(
            "BackupViewModel.kt",
            "BackupAction.kt",
            "BackupUiState.kt",
            "BackupSessionPolicy.kt",
            "BackupReducer.kt",
            "BackupOperationCoordinator.kt",
        ).map { name ->
            File("src/main/java/com/aozijx/passly/core/backup/$name")
        }
        val leakedBackupUi = File(
            "src/main/java/com/aozijx/passly/core/ui/components/backup"
        )

        assertTrue(
            "Core must not own Backup feature presentation/contracts",
            leakedPresentationFiles.none(File::exists)
        )
        assertTrue(
            "Backup-specific UI belongs to feature/backup, not core/ui",
            !leakedBackupUi.exists() || leakedBackupUi.walkTopDown().none {
                it.isFile && it.extension == "kt"
            }
        )
    }

    @Test
    fun backupMviSeparatesStateReductionFromSideEffects() {
        val presentationRoot = File(
            "src/main/java/com/aozijx/passly/feature/backup/internal/presentation"
        )
        val viewModel = File(presentationRoot, "BackupViewModel.kt").readText()
        val reducer = File(presentationRoot, "BackupReducer.kt")
        val coordinator = File(presentationRoot, "BackupOperationCoordinator.kt")
        val forbiddenViewModelDependencies = listOf(
            "AppSettingsRepository",
            "AuthenticationManager",
            "BackupStorageSupport",
            "BackupArchiveService",
        ).filter { dependency -> dependency in viewModel }

        assertTrue("Backup must have a dedicated pure reducer", reducer.exists())
        assertTrue("Backup must isolate side effects in a coordinator", coordinator.exists())
        assertTrue(
            "BackupViewModel must not directly orchestrate infrastructure: " +
                    forbiddenViewModelDependencies,
            forbiddenViewModelDependencies.isEmpty(),
        )
        assertTrue(
            "BackupViewModel must reduce mutations through BackupReducer",
            "BackupReducer.reduce" in viewModel,
        )
    }

    @Test
    fun backupPasswordStateIsWipeableAndClearedAtLifecycleBoundaries() {
        val contract = File(
            "src/main/java/com/aozijx/passly/feature/backup/internal/contract/" +
                    "BackupUiState.kt"
        ).readText()
        val action = File(
            "src/main/java/com/aozijx/passly/feature/backup/internal/contract/" +
                    "BackupAction.kt"
        ).readText()
        val viewModel = File(
            "src/main/java/com/aozijx/passly/feature/backup/internal/presentation/" +
                    "BackupViewModel.kt"
        ).readText()
        val coordinator = File(
            "src/main/java/com/aozijx/passly/feature/backup/internal/presentation/" +
                    "BackupOperationCoordinator.kt"
        ).readText()

        assertTrue(
            "Backup password contracts must not retain immutable String state",
            "backupPassword: SensitiveValue" in contract &&
                    "backupPassword: String" !in contract &&
                    "UpdatePassword(val password: SensitiveValue)" in action,
        )
        assertTrue(
            "Replacing, cancelling and clearing Backup must wipe password storage",
            "previous.wipe()" in viewModel &&
                    "clearPasswordAndMutate" in viewModel &&
                    "override fun onCleared()" in viewModel &&
                    "backupPassword.wipe()" in viewModel,
        )
        assertTrue(
            "Operation password copies must be wiped after use",
            "backupPassword.takeUnless { it.isEmpty }?.toCharArray()" in coordinator &&
                    "password?.fill('\\u0000')" in coordinator,
        )
    }

    @Test
    fun detailMviUsesAPureMutationReducer() {
        val viewModel = File(
            "src/main/java/com/aozijx/passly/feature/detail/DetailViewModel.kt"
        ).readText()
        val reducer = File(
            "src/main/java/com/aozijx/passly/feature/detail/internal/presentation/" +
                    "DetailReducer.kt"
        ).readText()
        val reducerDependencies = listOf(
            "Repository",
            "AuthorizationGate",
            "Context",
            "Navigation",
            "viewModelScope",
        ).filter(reducer::contains)

        assertTrue(
            "DetailViewModel must route state mutations through DetailReducer",
            "DetailReducer.reduce" in viewModel && "_uiState.update" !in viewModel,
        )
        assertTrue(
            "DetailReducer must remain a pure state transition: $reducerDependencies",
            "internal object DetailReducer" in reducer && reducerDependencies.isEmpty(),
        )
    }

    @Test
    fun vaultMviUsesOneStateOwnerAndAPureMutationReducer() {
        val viewModel = File(
            "src/main/java/com/aozijx/passly/feature/vault/VaultViewModel.kt"
        ).readText()
        val reducer = File(
            "src/main/java/com/aozijx/passly/feature/vault/presentation/VaultReducer.kt"
        ).readText()
        val reducerDependencies = listOf(
            "Repository",
            "Context",
            "Navigation",
            "viewModelScope",
            "MutableStateFlow",
        ).filter(reducer::contains)

        assertTrue(
            "VaultViewModel must route state mutations through VaultReducer",
            "VaultReducer.reduce" in viewModel &&
                    "SearchFilterState" !in viewModel &&
                    "VaultDialogState" !in viewModel,
        )
        assertTrue(
            "VaultReducer must remain a pure state transition: $reducerDependencies",
            "internal object VaultReducer" in reducer && reducerDependencies.isEmpty(),
        )
    }

    @Test
    fun authenticationMviKeepsSecretWipingOutsideItsPureReducer() {
        val viewModel = File(
            "src/main/java/com/aozijx/passly/feature/auth/presentation/" +
                    "AuthenticationViewModel.kt"
        ).readText()
        val reducer = File(
            "src/main/java/com/aozijx/passly/feature/auth/presentation/" +
                    "AuthenticationReducer.kt"
        ).readText()
        val reducerDependencies = listOf(
            "AuthenticationManager",
            "AuthenticationMethodProvisioner",
            "MemoryCleaner",
            "viewModelScope",
            "MutableStateFlow",
        ).filter(reducer::contains)

        assertTrue(
            "AuthenticationViewModel must use one reducer state-write entry",
            "AuthenticationReducer.reduce" in viewModel && "_uiState.update" !in viewModel,
        )
        assertTrue(
            "Secret cleanup must remain an explicit ViewModel side effect",
            ".wipe()" in viewModel && "MemoryCleaner.wipeCharArray" in viewModel,
        )
        assertTrue(
            "AuthenticationReducer must remain pure: $reducerDependencies",
            "internal object AuthenticationReducer" in reducer &&
                    ".wipe()" !in reducer &&
                    reducerDependencies.isEmpty(),
        )
    }

    @Test
    fun mainMviSeparatesShellStateFromDatabaseAndAuthenticationEffects() {
        val viewModel = File(
            "src/main/java/com/aozijx/passly/app/shell/AppShellViewModel.kt"
        ).readText()
        val reducer = File(
            "src/main/java/com/aozijx/passly/app/shell/presentation/AppShellReducer.kt"
        ).readText()
        val reducerDependencies = listOf(
            "AuthenticationManager",
            "DatabaseLifecycleUseCases",
            "SearchIndexMaintenance",
            "AppSettingsRepository",
            "viewModelScope",
            "MutableStateFlow",
        ).filter(reducer::contains)

        assertTrue(
            "AppShellViewModel must route shell state through AppShellReducer",
            "AppShellReducer.reduce" in viewModel && "_uiState.update" !in viewModel,
        )
        assertTrue(
            "AppShellReducer must not coordinate infrastructure: $reducerDependencies",
            "internal object AppShellReducer" in reducer && reducerDependencies.isEmpty(),
        )
        assertTrue(
            "Database and authentication effects must remain in AppShellViewModel",
            "databaseLifecycleUseCases" in viewModel &&
                    "authenticationManager.authenticate" in viewModel &&
                    "emitEffect" in viewModel,
        )
    }

    @Test
    fun settingsMviDoesNotExposeAuthenticationInfrastructureToUi() {
        val viewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/SettingsViewModel.kt"
        ).readText()
        val reducer = File(
            "src/main/java/com/aozijx/passly/feature/settings/presentation/SettingsReducer.kt"
        ).readText()
        val appPasswordFlows = File(
            "src/main/java/com/aozijx/passly/feature/settings/apppassword/" +
                    "AppPasswordFlows.kt"
        ).readText()
        val reducerDependencies = listOf(
            "AuthenticationManager",
            "DatabaseLifecycleUseCases",
            "AppSettingsRepository",
            "viewModelScope",
            "MutableStateFlow",
        ).filter(reducer::contains)

        assertTrue(
            "SettingsViewModel must route UI state through SettingsReducer",
            "SettingsReducer.reduce" in viewModel && "_uiState.update" !in viewModel,
        )
        assertTrue(
            "Settings authentication manager must remain private to its ViewModel",
            "private val authenticationManager" in viewModel &&
                    "settingsViewModel.authenticationManager" !in appPasswordFlows,
        )
        assertTrue(
            "SettingsReducer must remain pure: $reducerDependencies",
            "internal object SettingsReducer" in reducer && reducerDependencies.isEmpty(),
        )
    }

    @Test
    fun autofillFillUsesAnExplicitStateMachineAndIntentEntryPoint() {
        val viewModel = File(
            "src/main/java/com/aozijx/passly/feature/autofill/framework/" +
                    "AutofillFillViewModel.kt"
        ).readText()
        val reducer = File(
            "src/main/java/com/aozijx/passly/feature/autofill/framework/" +
                    "AutofillFillReducer.kt"
        ).readText()
        val activity = File(
            "src/main/java/com/aozijx/passly/feature/autofill/framework/" +
                    "AutofillFillActivity.kt"
        ).readText()
        val reducerDependencies = listOf(
            "CandidateResolver",
            "AutofillUseCases",
            "AppSettingsRepository",
            "AutofillRequestSession",
            "Context",
            "viewModelScope",
            "MutableStateFlow",
        ).filter(reducer::contains)

        assertTrue(
            "AutofillFillViewModel must expose one typed intent entry point",
            "fun onIntent(" in viewModel &&
                    "viewModel.initialize(" !in activity &&
                    "viewModel.selectCandidate(" !in activity,
        )
        assertTrue(
            "Autofill fill state transitions must use AutofillFillReducer",
            "AutofillFillReducer.reduce" in viewModel && "_uiState.update" !in viewModel,
        )
        assertTrue(
            "AutofillFillReducer must remain pure: $reducerDependencies",
            "internal object AutofillFillReducer" in reducer && reducerDependencies.isEmpty(),
        )
    }

    @Test
    fun recoveryModeMviKeepsPasswordResetScopedAndWipeable() {
        val viewModel = moduleSource(
            "com/aozijx/passly/feature/recovery/RecoveryModeViewModel.kt"
        ).readText()
        val uiState = moduleSource(
            "com/aozijx/passly/feature/recovery/contract/RecoveryModeUiState.kt"
        ).readText()
        val reducer = moduleSource(
            "com/aozijx/passly/feature/recovery/presentation/RecoveryModeReducer.kt"
        ).readText()
        val reducerDependencies = listOf(
            "AuthenticationManager",
            "AuthenticationMethodProvisioner",
            "MemoryCleaner",
            "viewModelScope",
            "MutableStateFlow",
        ).filter(reducer::contains)

        assertTrue(
            "Recovery password inputs must use wipeable state values",
            "SensitiveValue" in uiState &&
                    "newPassword: String" !in uiState &&
                    "confirmPassword: String" !in uiState,
        )
        assertTrue(
            "RecoveryModeViewModel must reduce state and explicitly wipe secrets",
            "RecoveryModeReducer.reduce" in viewModel &&
                    "_uiState.update" !in viewModel &&
                    "wipePasswords()" in viewModel,
        )
        assertTrue(
            "RecoveryModeReducer must remain pure: $reducerDependencies",
            "internal object RecoveryModeReducer" in reducer &&
                    ".wipe()" !in reducer &&
                    reducerDependencies.isEmpty(),
        )
        assertFalse(
            "Recovery mode must still have no backup or full-session capability",
            "Backup" in viewModel || "markAuthenticated" in viewModel,
        )
    }

    @Test
    fun scannerUsesALightweightPureStateReducer() {
        val viewModel = File(
            "src/main/java/com/aozijx/passly/feature/scanner/ScannerViewModel.kt"
        ).readText()
        val reducer = File(
            "src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerReducer.kt"
        ).readText()

        assertTrue(
            "Scanner state must transition only through ScannerReducer",
            "ScannerReducer.reduce" in viewModel && "_uiState.update" !in viewModel,
        )
        assertTrue(
            "Scanner reducer must not own Android or decoding side effects",
            "android." !in reducer &&
                    "QrCodeUtils" !in reducer &&
                    "Vibrator" !in reducer &&
                    "internal object ScannerReducer" in reducer,
        )
    }

    @Test
    fun createEntryUsesAGenericReducerWithoutOwningValidationRules() {
        val viewModel = File(
            "src/main/java/com/aozijx/passly/feature/vault/editor/common/" +
                    "CreateEntryViewModel.kt"
        ).readText()
        val reducer = File(
            "src/main/java/com/aozijx/passly/feature/vault/editor/common/" +
                    "CreateEntryReducer.kt"
        ).readText()

        assertTrue(
            "CreateEntry state must transition only through its generic reducer",
            "CreateEntryReducer.reduce" in viewModel && "_uiState.update" !in viewModel,
        )
        assertTrue(
            "Validation and infrastructure must stay outside CreateEntryReducer",
            "isFormValid" !in reducer &&
                    "EntryCommandRepository" !in reducer &&
                    "SecureSessionAccessState" !in reducer &&
                    "viewModelScope" !in reducer,
        )
        assertTrue(
            "CreateEntry access dependency must use secure-session semantics",
            "secureSessionAccessState" in viewModel && "vaultAccessState" !in viewModel,
        )
    }

    @Test
    fun credentialResponseSeparatesIntentParsingStateAndAuthenticationMapping() {
        val viewModel = File(
            "src/main/java/com/aozijx/passly/feature/autofill/credential/" +
                    "CredentialResponseViewModel.kt"
        ).readText()
        val activity = File(
            "src/main/java/com/aozijx/passly/feature/autofill/credential/" +
                    "CredentialResponseActivity.kt"
        ).readText()
        val parser = File(
            "src/main/java/com/aozijx/passly/feature/autofill/credential/" +
                    "CredentialRequestParser.kt"
        ).readText()
        val reducer = File(
            "src/main/java/com/aozijx/passly/feature/autofill/credential/" +
                    "CredentialResponseReducer.kt"
        ).readText()

        assertTrue(
            "Credential response Activity must use one typed intent entry point",
            "fun onIntent(" in viewModel &&
                    "viewModel.handlePassword" !in activity &&
                    "viewModel.handleUnlock" !in activity &&
                    "viewModel.rejectUnknownAction" !in activity,
        )
        assertTrue(
            "Android Credential Manager request parsing belongs in its parser",
            "CredentialRequestParser.parsePasswordGet" in viewModel &&
                    "retrieveProviderGetCredentialRequest" !in viewModel &&
                    "retrieveProviderGetCredentialRequest" in parser,
        )
        assertTrue(
            "Credential response state must transition only through its reducer",
            "CredentialResponseReducer.reduce" in viewModel &&
                    Regex("_state\\.value\\s*=").findAll(viewModel).count() == 1,
        )
        assertTrue(
            "Credential response reducer must not own parsing or authentication",
            "PendingIntentHandler" !in reducer &&
                    "AuthenticationResult" !in reducer &&
                    "CredentialResponseUseCases" !in reducer,
        )
    }

    @Test
    fun recoveryDraftUsesAConstrainedPureStateMachine() {
        val viewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/security/" +
                    "RecoveryDraftViewModel.kt"
        ).readText()
        val reducer = File(
            "src/main/java/com/aozijx/passly/feature/settings/security/presentation/" +
                    "RecoveryDraftReducer.kt"
        ).readText()

        assertTrue(
            "Recovery draft state must have one reducer write path",
            "RecoveryDraftReducer.reduce" in viewModel &&
                    Regex("_state\\.value\\s*=").findAll(viewModel).count() == 1,
        )
        assertTrue(
            "Recovery draft reducer must constrain ready and committed transitions",
            "expected = RecoveryDraftState.Generating" in reducer &&
                    "state is RecoveryDraftState.Ready" in reducer,
        )
        assertTrue(
            "Draft lifecycle side effects must stay in the ViewModel",
            "SavedStateHandle" !in reducer &&
                    "AuthenticationManager" !in reducer &&
                    ".clear()" !in reducer,
        )
        assertTrue(
            "Recovery draft commands must use one typed action boundary",
            "fun onAction(action: RecoveryDraftAction)" in viewModel &&
                    Regex("""\n\s*fun (?:generate|confirmAndEnable|dismiss)\(""")
                        .containsMatchIn(viewModel)
                        .not(),
        )
    }

    @Test
    fun dataManagementUsesOneUiStateAndPureReducer() {
        val viewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/datamanagement/" +
                    "DataManagementSettingsViewModel.kt"
        ).readText()
        val reducer = File(
            "src/main/java/com/aozijx/passly/feature/settings/datamanagement/" +
                    "DataManagementSettingsReducer.kt"
        ).readText()

        assertTrue(
            "Data management state must use one uiState reducer path",
            "DataManagementSettingsReducer.reduce" in viewModel &&
                    "val uiState:" in viewModel &&
                    "_config" !in viewModel,
        )
        assertTrue(
            "Data management reducer must not own repositories or access checks",
            "Repository" !in reducer &&
                    "SecureSessionAccessState" !in reducer &&
                    "hasFullSecureSessionAccess" !in reducer,
        )
        assertTrue(
            "Data management access naming must use secure-session semantics",
            "secureSessionAccessState" in viewModel && "vaultAccessState" !in viewModel,
        )
    }

    @Test
    fun securitySettingsUsesOneUiStateAndPureReducer() {
        val viewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/security/" +
                    "SecuritySettingsViewModel.kt"
        ).readText()
        val reducer = File(
            "src/main/java/com/aozijx/passly/feature/settings/security/presentation/" +
                    "SecuritySettingsReducer.kt"
        ).readText()
        val contract = File(
            "src/main/java/com/aozijx/passly/feature/settings/security/" +
                    "SecuritySettingsContract.kt"
        ).readText()

        assertTrue(
            "Security settings state must use one reducer write path",
            "SecuritySettingsReducer.reduce" in viewModel &&
                    "val uiState:" in viewModel &&
                    "val config:" !in viewModel &&
                    "val verifyResult:" !in viewModel &&
                    "val hasRecoveryEnvelope:" !in viewModel,
        )
        assertTrue(
            "Security commands must enter through SecuritySettingsAction",
            "SetBiometricEnabled" in contract &&
                    "SetInvalidateKeyOnBiometricChange" in contract &&
                    "fun setBiometricEnabled" !in viewModel.substringBefore("private fun"),
        )
        assertTrue(
            "Recovery verification credentials must remain wipeable",
            "VerifyRecoveryCode(val code: CharArray)" in contract &&
                    "action.code.toCharArray()" !in viewModel,
        )
        assertTrue(
            "Security reducer must remain free of authentication and repository side effects",
            "AuthenticationManager" !in reducer &&
                    "MethodProvisioner" !in reducer &&
                    "Repository" !in reducer,
        )
    }

    @Test
    fun viewModelMutableStateFlowsHaveOneWriteSite() {
        val declaration = Regex(
            """private\s+val\s+(_[A-Za-z0-9]+)(?:\s*:[^=]+)?\s*=\s*MutableStateFlow"""
        )
        val offenders = productionKotlinFiles
            .filter { "/feature/" in it.invariantSeparatorsPath }
            .filter { it.name.endsWith("ViewModel.kt") }
            .flatMap { file ->
                val source = file.readText()
                declaration.findAll(source).mapNotNull { match ->
                    val stateName = match.groupValues[1]
                    val write = Regex(
                        """\b${Regex.escape(stateName)}\.(?:""" +
                                """value\s*(?:[+*/-]?=|\+\+|--)|""" +
                                """update\s*\{|emit\s*\(|tryEmit\s*\()"""
                    )
                    val writeSites = write.findAll(source).count()
                    if (writeSites > 1) {
                        "${file.relativeTo(projectRoot).path}:$stateName ($writeSites writes)"
                    } else {
                        null
                    }
                }
            }
            .toList()

        assertTrue(
            "MutableStateFlow state must have one centralized write site: $offenders",
            offenders.isEmpty(),
        )
    }

    @Test
    fun createEntryEditorsUseTypedActionBoundaries() {
        val editorRoot = File(
            "src/main/java/com/aozijx/passly/feature/vault/editor"
        )
        val baseViewModel = File(editorRoot, "common/CreateEntryViewModel.kt").readText()
        val editorNames = listOf("password/AddPassword", "otp/AddOtp", "bankcard/AddBankCard")
        val offenders = editorNames.mapNotNull { editor ->
            val name = editor.substringAfterLast('/')
            val directory = editor.substringBeforeLast('/')
            val viewModel = File(editorRoot, "$directory/${name}ViewModel.kt").readText()
            val action = File(editorRoot, "$directory/${name}Action.kt").readText()
            val screen = File(editorRoot, "$directory/${name}Screen.kt").readText()
            val hasTypedBoundary = "fun onAction(action: ${name}Action)" in viewModel &&
                    "viewModel.onAction(" in screen &&
                    "viewModel.save()" !in screen &&
                    "viewModel.update" !in screen &&
                    "viewModel.onField" !in screen &&
                    "->" !in action.substringAfter("sealed interface")
            if (hasTypedBoundary) null else name
        }

        assertTrue(
            "Create-entry screens must dispatch data-only typed actions: $offenders",
            offenders.isEmpty(),
        )
        assertTrue(
            "Shared save implementation must not remain a public UI command",
            "protected fun saveEntry()" in baseViewModel &&
                    Regex("""\n\s*fun save\(""").containsMatchIn(baseViewModel).not(),
        )
    }

    @Test
    fun diagnosticsWorkflowIsOwnedByOneMviState() {
        val viewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/general/" +
                    "DiagnosticsSettingsViewModel.kt"
        ).readText()
        val reducer = File(
            "src/main/java/com/aozijx/passly/feature/settings/general/" +
                    "DiagnosticsSettingsReducer.kt"
        ).readText()
        val ui = File(
            "src/main/java/com/aozijx/passly/feature/settings/general/LogSettingsSection.kt"
        ).readText()

        assertTrue(
            "Diagnostics state must transition through its reducer",
            "DiagnosticsSettingsReducer.reduce" in viewModel &&
                    "val uiState:" in viewModel &&
                    "fun onAction(action: DiagnosticsSettingsAction)" in viewModel,
        )
        assertTrue(
            "Diagnostics UI must not own workflow state or call imperative ViewModel methods",
            "mutableStateOf" !in ui &&
                    "viewModel.readPage" !in ui &&
                    "viewModel.clear" !in ui &&
                    "viewModel.authenticateAndExport" !in ui &&
                    "viewModel.setFileLoggingEnabled" !in ui,
        )
        assertTrue(
            "Diagnostics reducer must stay free of runtime, authentication and export effects",
            "DiagnosticsRuntimeController" !in reducer &&
                    "AuthenticationManager" !in reducer &&
                    "DiagnosticsExportService" !in reducer,
        )
    }

    @Test
    fun interactionSettingsHasNoImperativeUiBypass() {
        val viewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/interaction/" +
                    "InteractionSettingsViewModel.kt"
        ).readText()
        val routes = File(
            "src/main/java/com/aozijx/passly/feature/settings/navigation/" +
                    "SettingsDataRoutes.kt"
        ).readText()

        assertTrue(
            "Interaction settings must expose uiState and dispatch platform actions",
            "val uiState:" in viewModel &&
                    "val config:" !in viewModel &&
                    "InteractionSettingsAction.OpenSystemAutofillSettings" in viewModel,
        )
        assertTrue(
            "Settings UI must not call an imperative autofill-settings method",
            "interactionViewModel.openAutofillSettings" !in routes,
        )
    }

    @Test
    fun mviViewModelsHaveOneActionEntryPoint() {
        // 只检查有对应 Intent/Action 合约文件的 ViewModel（完整 MVI 页面）。
        // 简单 UDF 页面（仅有 UiState）不强制统一事件入口。
        // 兼容迁移中的 onIntent/handleIntent，新代码使用 onAction。
        val offenders = productionKotlinFiles
            .filter { "/feature/" in it.invariantSeparatorsPath }
            .filter { it.name.endsWith("ViewModel.kt") }
            .filter { viewModelFile ->
                // 在 feature 目录树中查找对应的事件合约文件
                // 从 feature 根目录开始搜索，确保找到 contract/ 子目录下的文件
                val vmName = viewModelFile.name.removeSuffix("ViewModel.kt")
                val featureRoot = findFeatureRoot(viewModelFile)
                featureRoot != null && featureRoot.walkTopDown()
                    .any { f ->
                        f.isFile && f.extension == "kt" &&
                                (
                                        f.name == "${vmName}Intent.kt" ||
                                                f.name == "${vmName}Action.kt" ||
                                                f.name == "${vmName}UiAction.kt" ||
                                                (
                                                        f.name == "${vmName}Contract.kt" &&
                                                                (
                                                                        "sealed interface ${vmName}Action" in f.readText() ||
                                                                                "sealed interface ${vmName}Intent" in f.readText()
                                                                        )
                                                        )
                                        )
                    }
            }
            .filter { viewModelFile ->
                val text = viewModelFile.readText()
                "fun onAction(" !in text &&
                        "fun onIntent(" !in text &&
                        "fun handleIntent(" !in text
            }
            .map { it.relativeTo(projectRoot).path }
            .toList()

        assertTrue(
            "MVI ViewModels must expose one action/intent entry point: $offenders",
            offenders.isEmpty()
        )
    }

    /**
     * 从 ViewModel 文件向上查找 feature 根目录（feature/<name>/ 的直接父目录）。
     * 返回 feature 根目录，例如 feature/main/。
     */
    private fun findFeatureRoot(viewModelFile: File): File? {
        val featureBase = File("src/main/java/com/aozijx/passly/feature")
        var current: File? = viewModelFile.parentFile
        while (current != null && current.canonicalPath != featureBase.canonicalPath) {
            if (current.parentFile?.canonicalPath == featureBase.canonicalPath) {
                return current
            }
            current = current.parentFile
        }
        return null
    }

    @Test
    fun productionFilesDoNotExceedReasonableSize() {
        val maxLines = 500
        val offenders = productionKotlinFiles
            .mapNotNull { file ->
                val lineCount = file.useLines { it.count() }
                if (lineCount > maxLines) {
                    "${file.relativeTo(projectRoot).path} ($lineCount lines)"
                } else {
                    null
                }
            }
            .toList()

        assertTrue(
            "Files exceeding $maxLines lines (should be split by responsibility): $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun vaultFeatureHasExplicitIntentContract() {
        val vaultIntentFile = File(
            "src/main/java/com/aozijx/passly/feature/vault/contract/VaultIntent.kt"
        )
        val vaultViewModel = File(
            "src/main/java/com/aozijx/passly/feature/vault/VaultViewModel.kt"
        ).readText()

        assertTrue(
            "Vault feature must have VaultIntent.kt contract",
            vaultIntentFile.exists()
        )
        assertTrue(
            "VaultViewModel must use onIntent as single entry point",
            "fun onIntent(" in vaultViewModel
        )
    }

    @Test
    fun settingsViewModelHasExplicitIntentContract() {
        val settingsViewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/SettingsViewModel.kt"
        )
        if (settingsViewModel.exists()) {
            val text = settingsViewModel.readText()
            val hasIntentContract = File(
                "src/main/java/com/aozijx/passly/feature/settings/contract/SettingsIntent.kt"
            ).exists()

            assertTrue(
                "Settings feature must have SettingsIntent.kt contract",
                hasIntentContract
            )
            assertTrue(
                "SettingsViewModel must use onIntent/handleIntent as entry point",
                "fun onIntent(" in text || "fun handleIntent(" in text
            )
        }
    }

    @Test
    fun settingsUsesOneAnimatedAdaptiveNavigationOwner() {
        val settingsNavigation = File(
            "src/main/java/com/aozijx/passly/feature/settings/navigation/SettingsNavGraph.kt"
        ).readText()

        assertTrue(
            "Settings must use the navigable adaptive scaffold",
            "NavigableListDetailPaneScaffold(" in settingsNavigation
        )
        assertTrue(
            "Both settings panes must opt into adaptive pane transitions",
            Regex("AnimatedPane\\s*[({]").findAll(settingsNavigation).count() >= 2
        )
        assertTrue(
            "Settings must not keep a second Navigation Compose owner inside the adaptive scaffold",
            "rememberNavController(" !in settingsNavigation && "NavHost(" !in settingsNavigation
        )
        assertTrue(
            "Settings back navigation must only consume actual pane changes",
            "BackNavigationBehavior.PopUntilScaffoldValueChange" in settingsNavigation
        )
        assertTrue(
            "Phone transitions must be selected from the adaptive partition count",
            "scaffoldDirective.maxHorizontalPartitions == 1" in settingsNavigation
        )
        assertTrue(
            "Phone push transitions belong to pane visibility, not expanded detail content",
            "AnimatedContent(" !in settingsNavigation
        )
        assertTrue(
            "Selection state must be independent from pane navigation history",
            "var selectedRoute by rememberSaveable" in settingsNavigation &&
                    "lastDetailRoute" !in settingsNavigation
        )
        assertTrue(
            "The detail pane must remain above the list while either pane is moving",
            "modifier = Modifier.zIndex(1f)" in settingsNavigation
        )
    }

    @Test
    fun stringTextFieldsDoNotRecreateTextFieldValueAndLoseSelection() {
        val offenders = productionKotlinFiles
            .filter { "value = TextFieldValue(" in it.readText() }
            .map { it.relativeTo(projectRoot).path }
            .toList()

        assertTrue(
            "String-backed text fields must use the String overload to preserve cursor selection: " +
                    offenders,
            offenders.isEmpty()
        )
    }

    @Test
    fun highSensitivityReadsRequireScopedSingleUsePermits() {
        val detailViewModel = File(
            "src/main/java/com/aozijx/passly/feature/detail/DetailViewModel.kt"
        ).readText()
        val detailReducer = File(
            "src/main/java/com/aozijx/passly/feature/detail/internal/presentation/" +
                    "DetailReducer.kt"
        ).readText()
        val sensitiveRepository = moduleSource(
            "com/aozijx/passly/data/repository/entry/RoomSensitiveFieldRepository.kt"
        ).readText()
        val sessionController = File(
            "src/main/java/com/aozijx/passly/security/authentication/VaultSessionController.kt"
        ).readText()

        assertTrue(
            "Detail must authorize the exact entry and sensitive-field set",
            "AuthorizationScope.SensitiveFields" in detailViewModel &&
                    "authorizationGate.authorize" in detailViewModel
        )
        assertTrue(
            "Sensitive repository must consume the permit before decryption",
            "permitVerifier.consume" in sensitiveRepository &&
                    "AuthorizationScope.SensitiveFields" in sensitiveRepository
        )
        assertTrue(
            "Every lock path must revoke outstanding authorization permits",
            "authorizationPermitRevoker.revokeAll()" in sessionController
        )

        val detailSections = File(
            "src/main/java/com/aozijx/passly/feature/detail/ui/sections"
        ).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue(
            "High-sensitivity UI must dispatch typed intents instead of authenticating around plaintext",
            detailSections.none { "SensitiveAccessLevel.HIGH" in it.readText() }
        )
        assertTrue(
            "Detail must load non-secret field presence before rendering protected values",
            "sensitiveFieldRepository.getPresence" in detailViewModel &&
                    "sensitiveFieldKeys" in detailReducer
        )
    }

    @Test
    fun entryRevisionsStoreCompleteSnapshotsWithoutHighSensitivityPlaintext() {
        val helper = moduleSource(
            "com/aozijx/passly/data/repository/entry/command/EntryRevisionWriter.kt"
        ).readText()
        val updateExecutor = moduleSource(
            "com/aozijx/passly/data/repository/entry/command/UpdateEntryExecutor.kt"
        ).readText()
        val revisionEntity = File(
            "../data/src/main/java/com/aozijx/passly/data/local/database/entity/EntryRevisionEntity.kt"
        ).readText()

        assertTrue(
            "Revision helper must capture links, attachment refs and existing high-field ciphertext",
            "entryLinkQueryDao().getByEntryId" in helper &&
                    "attachmentRefQueryDao().getCommittedByEntryId" in helper &&
                    "sensitiveFieldQueryDao().getFields" in helper
        )
        assertTrue(
            "Revision writes must use the validated single-credential secret",
            "secret = newSecret" in updateExecutor.substringAfter("snapshotChanges(")
        )
        assertTrue(
            "Revision storage must separate regular encrypted snapshot from high-field ciphertexts",
            "entryContentCipher" in revisionEntity &&
                    "sensitiveFieldCipherSet" in revisionEntity
        )
        assertTrue("Each entry must retain at most 50 revisions", "REVISION_LIMIT = 50" in helper)

        val linkRepository = moduleSource(
            "com/aozijx/passly/data/repository/entry/RoomEntryLinkRepository.kt"
        ).readText()
        assertTrue(
            "Link upsert and delete must snapshot every affected endpoint",
            linkRepository.split("revisionHelper.snapshotCurrent").size - 1 >= 2 &&
                    "previous?.let" in linkRepository
        )
    }

    @Test
    fun composeLazyListsUseBundleSaveableEntryKeys() {
        val lazyListFiles = listOf(
            File(
                "src/main/java/com/aozijx/passly/feature/vault/components/list/" +
                        "VaultPagerContent.kt"
            ),
            File(
                "src/main/java/com/aozijx/passly/feature/settings/datamanagement/" +
                        "TrashBottomSheet.kt"
            )
        )
        val sources = lazyListFiles.associateWith(File::readText)

        assertTrue(
            "Compose lazy-list keys must not pass the EntryId value object to SaveableStateHolder",
            sources.values.none { "key = EntryListItem::id" in it }
        )
        assertTrue(
            "Every entry lazy-list key must unwrap EntryId to its Bundle-saveable String value",
            sources.values.all { ".id.value" in it }
        )
    }
}
