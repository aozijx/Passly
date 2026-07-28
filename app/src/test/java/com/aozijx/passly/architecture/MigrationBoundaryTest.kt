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
        val exemptPaths = listOf("/core/session/")
        val offenders = productionKotlinFiles
            .filter { source ->
                guardedPackages.any { it in source.invariantSeparatorsPath }
            }
            .filter { source ->
                exemptPaths.none { it in source.invariantSeparatorsPath }
            }
            .filter { "import com.aozijx.passly.data." in it.readText() }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue("Data implementation leaks: $offenders", offenders.isEmpty())
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
    fun vaultMenuStateIsTransientAndPasswordCreationUsesItsOwnRoute() {
        val vaultUiState = File(
            "src/main/java/com/aozijx/passly/feature/vault/contract/VaultUiState.kt"
        ).readText()
        val searchFilterState = File(
            "src/main/java/com/aozijx/passly/feature/vault/list/SearchFilterState.kt"
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
        val oldPasswordDialog = File(
            "src/main/java/com/aozijx/passly/feature/vault/components/editor/" +
                    "AddPasswordDialog.kt"
        )

        assertTrue(
            "Popup visibility must not survive in VaultViewModel state",
            "isMoreMenuExpanded" !in vaultUiState &&
                    "isMoreMenuExpanded" !in searchFilterState
        )
        assertTrue(
            "Top bar must own popup visibility locally",
            "var isMoreMenuExpanded by remember" in topBar
        )
        assertTrue(
            "Password creation must have a dedicated navigation route",
            "data object AddPassword" in routeSource && passwordScreen.isFile
        )
        assertTrue(
            "Password dialog must not return",
            !oldPasswordDialog.exists()
        )
    }

    @Test
    fun associatedPackagesStayEncryptedAndVisibleInEntryDetails() {
        val entryEntity = File(
            "src/main/java/com/aozijx/passly/data/model/entity/EntryEntity.kt"
        ).readText()
        val summaryPayload = File(
            "src/main/java/com/aozijx/passly/data/model/payload/summary/SummaryPayload.kt"
        ).readText()
        val loginAssociationCard = File(
            "src/main/java/com/aozijx/passly/feature/detail/sections/" +
                    "LoginDomainIconCard.kt"
        ).readText()

        assertTrue(
            "Associated packages must remain inside encrypted summary payloads",
            "packageNames: Set<String>" in summaryPayload &&
                    "val packageName" !in entryEntity
        )
        assertTrue(
            "Login details must expose associated package names",
            "entry.website?.packageNames" in loginAssociationCard &&
                    "AssociatedPackageRow" in loginAssociationCard
        )
    }

    @Test
    fun appPasswordFeedbackAndEntryAuthenticationStaySeparated() {
        val passwordFields = File(
            "src/main/java/com/aozijx/passly/feature/settings/apppassword/ui/" +
                    "PasswordFields.kt"
        ).readText()
        val authenticationHost = File(
            "src/main/java/com/aozijx/passly/feature/auth/ui/host/AuthenticationHost.kt"
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
        val authenticationModels = File(
            "src/main/java/com/aozijx/passly/domain/authentication/AuthenticationModels.kt"
        ).readText()
        val mainScreen = File(
            "src/main/java/com/aozijx/passly/feature/main/ui/MainScreen.kt"
        ).readText()
        val inputActionButton = File(
            "src/main/java/com/aozijx/passly/core/ui/components/common/" +
                    "InputActionButton.kt"
        ).readText()
        val authenticationViewModel = File(
            "src/main/java/com/aozijx/passly/feature/auth/presentation/" +
                    "AuthenticationViewModel.kt"
        ).readText()
        val authenticationUiState = File(
            "src/main/java/com/aozijx/passly/feature/auth/presentation/" +
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
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue(
            "Compose outside UI packages or feature Screen entry points: $offenders",
            offenders.isEmpty()
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
            "src/main/java/com/aozijx/passly/core/ui/components/settings/SettingsSection.kt"
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
    fun backupUiLivesInSettingsAndUsesBottomSheets() {
        val vaultRoot = File("src/main/java/com/aozijx/passly/feature/vault")
        val vaultOffenders = vaultRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter {
                val text = it.readText()
                "BackupViewModel" in text ||
                    "BackupIntent" in text ||
                    "BackupPasswordDialog" in text ||
                    "CustomExportMenuItem" in text
            }
            .map { it.relativeTo(vaultRoot).path }
            .toList()
        val sheetSource = File(
            "src/main/java/com/aozijx/passly/feature/settings/datamanagement/BackupRestoreSheets.kt"
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
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue("Legacy message/permission references: $offenders", offenders.isEmpty())
    }

    @Test
    fun domainDoesNotEmitTelemetryOrUserMessages() {
        val forbidden = listOf("TelemetryEmitter", "AppNoticePublisher", "AppLog")
        val offenders = productionKotlinFiles
            .filter { "/domain/" in it.invariantSeparatorsPath }
            .filter { "/domain/notice/port/" !in it.invariantSeparatorsPath }
            .filter { source -> forbidden.any { it in source.readText() } }
            .map { it.relativeTo(File("src/main/java")).path }
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
            .map { it.relativeTo(File("src/main/java")).path }
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
    fun destructiveDatabaseRecoveryRequiresFreshAuthentication() {
        val mainViewModel = File(
            "src/main/java/com/aozijx/passly/feature/main/MainViewModel.kt"
        ).readText()
        val settingsViewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/SettingsViewModel.kt"
        ).readText()
        val recoveryDialog = File(
            "src/main/java/com/aozijx/passly/core/ui/components/DatabaseRecoveryDialog.kt"
        ).readText()
        val controller = File(
            "src/main/java/com/aozijx/passly/data/repository/database/DatabaseControllerImpl.kt"
        ).readText()
        val recoveryStore = File(
            "src/main/java/com/aozijx/passly/data/local/database/maintenance/" +
                "DatabaseRecoveryStore.kt"
        ).readText()
        val authenticationManager = File(
            "src/main/java/com/aozijx/passly/security/authentication/DefaultAuthenticationManager.kt"
        ).readText()
        val credentialExecutor = File(
            "src/main/java/com/aozijx/passly/security/authentication/CredentialMethodExecutor.kt"
        ).readText()
        val sessionController = File(
            "src/main/java/com/aozijx/passly/security/authentication/VaultSessionController.kt"
        ).readText()
        val recoveryCompletion =
            sessionController.substringAfter("suspend fun completeDatabaseRecovery")

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
            "Recovery code must remain available when Room cannot open",
            "AuthenticationPurpose.RECOVER_DATABASE" in authenticationManager &&
                "AuthenticationPurpose.CLEAR_DATABASE -> AuthenticationMethod.entries.toSet()" in
                authenticationManager
        )
        assertTrue(
            "Cold-start recovery must stage the DEK without opening the broken database",
            "session.stageDatabaseRecovery(type, ownedDek)" in credentialExecutor
        )
        assertTrue(
            "A recovered database must open before authentication state is published",
            recoveryCompletion.indexOf("sessionManager.lockState != LockState.UNLOCKED") <
                recoveryCompletion.indexOf("markAuthenticatedInternal()")
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
            "if (lockLevel == VaultLockState.SEALED)" in sessionController &&
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
        assertTrue("Recovery draft must use its domain factory", "RecoveryCodeDraftFactory" in source)
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
            .map { it.relativeTo(File("src/main/java")).path }
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
        val source = File(
            "src/main/java/com/aozijx/passly/data/diagnostics/EncryptedLogStore.kt"
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
            .map { it.relativeTo(File("src/main/java")).path }
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
            .map { it.relativeTo(File("src/main/java")).path }
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
            .map { it.relativeTo(File("src/main/java")).path }
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
            File("src/main/java/com/aozijx/passly/data/notice"),
            File("src/main/java/com/aozijx/passly/data/diagnostics"),
            File("src/main/java/com/aozijx/passly/domain/notice")
        )
        val offenders = scopedRoots.asSequence()
            .filter(File::exists)
            .flatMap { it.walkTopDown() }
            .filter { it.isFile && it.extension == "kt" }
            .filter {
                val text = it.readText()
                "TODO" in text || "FIXME" in text || "StubSystemNotificationGateway" in text
            }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue("Incomplete center implementations: $offenders", offenders.isEmpty())
    }
}
